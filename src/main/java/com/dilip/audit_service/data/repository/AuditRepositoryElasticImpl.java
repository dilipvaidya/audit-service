package com.dilip.audit_service.data.repository;

import com.dilip.audit_service.common.DeletionResult;
import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.dilip.audit_service.data.entity.UserContext;
import com.dilip.audit_service.exception.NotFoundException;
import com.dilip.audit_service.services.UserContextService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.reindex.BulkByScrollResponse;
import org.elasticsearch.index.reindex.DeleteByQueryRequest;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository("elasticsearch")
@Profile({"dev", "prod"})
public class AuditRepositoryElasticImpl implements AuditRepository {

    private static final Logger log = LoggerFactory.getLogger(AuditRepositoryElasticImpl.class);
    private final RestHighLevelClient elasticClient;
    private final ObjectMapper mapper;
    private static final String INDEX = "audit-logs";
    private final UserContextService userContextService;


    @Autowired
    public AuditRepositoryElasticImpl(RestHighLevelClient elasticClient, ObjectMapper mapper, UserContextService userContextService) {
        this.elasticClient = elasticClient;
        this.mapper = mapper;
        this.userContextService = userContextService;
    }

    @Override
    public void save(AuditLog auditLog) {
        try {
            IndexRequest request = new IndexRequest(INDEX)
                    .id(auditLog.getEventId())
                    .source(mapper.writeValueAsString(auditLog), XContentType.JSON);
            elasticClient.index(request, RequestOptions.DEFAULT);
        } catch (Exception e) {
            throw new RuntimeException("failed to index audit log", e);
        }

    }

    @Override
    public List<AuditLog> findAll() {
        AuditSearchRequest auditSearchRequest = new AuditSearchRequest();

        List<AuditLog> auditLog = this.advancedSearch(auditSearchRequest);
        if(auditLog.isEmpty()) {
            return List.of();
        }

        return auditLog;
    }

    public List<AuditLog> findByUserId(String userId) {
        AuditSearchRequest auditSearchRequest = new AuditSearchRequest();
        auditSearchRequest.setChangedByUserId(userId);

        List<AuditLog> auditLog = this.advancedSearch(auditSearchRequest);
        if(auditLog.isEmpty()) {
            return List.of();
        }

        // verify its same user Id in all the audit logs received as a list.
        if(!userId.equals(auditLog.get(0).getChangedBy().getUserId())) {
            throw new NotFoundException();
        }
        return auditLog;
    }

    @Override
    public Optional<AuditLog> findByEventId(String eventId) {
        AuditSearchRequest auditSearchRequest = new AuditSearchRequest();
        auditSearchRequest.setEventId(eventId);

        List<AuditLog> auditLog = this.advancedSearch(auditSearchRequest);
        if(auditLog.isEmpty()) {
            return Optional.empty();
        }

        // verify its same event Id log
        if(!eventId.equals(auditLog.get(0).getEventId())) {
            // todo: along with exception, need to look at the logic why log with different event Id is being pulled.
            throw new NotFoundException();
        }
        return Optional.of(auditLog.get(0));
    }

    @Override
    public List<AuditLog> advancedSearch(AuditSearchRequest request) {

        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        UserContext user = userOpt.orElseThrow(() ->
                new AccessDeniedException("failed to perform advanced search: unauthenticated request")
        );

        if(request.getChangedByUserId() != null &&
                !user.isAdmin() &&
                !Objects.equals(request.getChangedByUserId(), user.getUserId())) {
            // non admin user can only access audit logs for his own userId.
            // which means for non-admin user the, the userId in the request parameter is mandatory and should match
            // with the userId of the logged user.
            throw new AccessDeniedException("failed to perform advanced search: unauthorised request");
        }

        try {
            BoolQueryBuilder query = QueryBuilders.boolQuery();

            if(request.getEntityType() != null) {
                query.must(QueryBuilders.termQuery("entityType", request.getEntityType()));
            }
            if(request.getEntityId() != null) {
                query.must(QueryBuilders.termQuery("entityId", request.getEntityId()));
            }
            if(request.getEventId() != null) {
                query.must(QueryBuilders.matchQuery("eventId", request.getEventId()));
            }
            if(request.getEventType() != null) {
                query.must(QueryBuilders.termQuery("eventType", request.getEventType()));
            }
            if(request.getSourceService() != null) {
                query.must(QueryBuilders.termQuery("sourceService", request.getSourceService()));
            }
            if(user.isAdmin() && request.getChangedByUserId() != null) {
                    query.must(QueryBuilders.matchQuery("changedBy.userId", request.getChangedByUserId()));
            }
            if(!user.isAdmin()) {
                log.debug("adding non-admin user: {}", user.getUserId());
                query.must(QueryBuilders.matchQuery("changedBy.userId", user.getUserId()));
            }


            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(query)
                    .from(request.getPage()*request.getSize())
                    .size(request.getSize());

            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(sourceBuilder);

            SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);

            List<AuditLog> results = new ArrayList<>();
            for(SearchHit hit : response.getHits()) {
                results.add(mapper.readValue(hit.getSourceAsString(), AuditLog.class));
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("failed to perform advanced search: unknown exception while querying transient database", e);
        }
    }

    @Override
    public DeletionResult deleteEvent(String eventId) {
        log.debug("Attempting to delete audit event with ID: {}", eventId);

        // for admin user, can delete any eventId
        // for non-admin user, can only delete event he has access to.
        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        if (userOpt.isEmpty()) { // authentication header in the request is empty.
            throw new AccessDeniedException("could not delete from transient database: Unauthenticated request");
        }
        UserContext user = userOpt.get();

        // Only admin user can delete any audit logs; others only their own
        if (!user.isAdmin()) {
            AuditLog auditLog = this.findByEventId(eventId)
                    .orElseThrow(() -> new AccessDeniedException("Unauthenticated request"));

            String changedByUserId = auditLog.getChangedBy().getUserId();
            String currentUserId = user.getUserId();

            log.debug("AuditLog changedByUserId: {}, Current userId: {}", changedByUserId, currentUserId);

            if (!changedByUserId.equals(currentUserId)) {
                throw new AccessDeniedException("could not delete from transient database: Unauthenticated request");
            }
        }

        try {
            DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX);
            request.setQuery(QueryBuilders.termQuery("eventId", eventId));

            BulkByScrollResponse response = this.elasticClient.deleteByQuery(request, RequestOptions.DEFAULT);
            long deletedCount = response.getDeleted();
            if(deletedCount > 0) {
                return new DeletionResult.DeletionSuccess(deletedCount);
            } else {
                return new DeletionResult.DeletionNotFound("could not delete from transient database: Not Found");
            }
        } catch (Exception e) {
            return new DeletionResult.DeletionFailure(e.getMessage(), e);
        }
    }
}
