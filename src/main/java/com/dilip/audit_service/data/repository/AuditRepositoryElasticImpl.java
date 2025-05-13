package com.dilip.audit_service.data.repository;

import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.dilip.audit_service.data.entity.UserContext;
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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository("elasticsearch")
@Profile({"dev", "prod"})
public class AuditRepositoryElasticImpl implements AuditRepository {

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
        try {
            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(new SearchSourceBuilder().query(QueryBuilders.matchAllQuery()));
            SearchResponse response = elasticClient.search(searchRequest, RequestOptions.DEFAULT);

            List<AuditLog> results = new ArrayList<>();
            for( SearchHit hit : response.getHits()) {
                results.add(mapper.readValue(hit.getSourceAsString(), AuditLog.class));
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all audit logs", e);
        }
    }

    public List<AuditLog> findByUserId(String userId) {
        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        if(userOpt.isEmpty()) {
            throw new AccessDeniedException("Unauthenticated request");
        }
        UserContext user = userOpt.get();

        try {
            BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();

            if(user.isAdmin() && userId != null) { //only admin can query with other users names
                queryBuilder.must(QueryBuilders.matchQuery("changedBy.userId", userId));
            }
            if (!user.isAdmin()) { // if not admin, we must filter by user.
                queryBuilder.must(QueryBuilders.matchQuery("changedBy.userId", userId));
            }

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                    .query(queryBuilder);

            SearchRequest searchRequest = new SearchRequest(INDEX);
            searchRequest.source(sourceBuilder);

            SearchResponse searchResponse = elasticClient.search(searchRequest, RequestOptions.DEFAULT);

            List<AuditLog> results = new ArrayList<>();
            for(SearchHit hit : searchResponse.getHits()) {
                results.add(mapper.readValue(hit.getSourceAsString(), AuditLog.class));
            }

            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve all audit logs by userId", e);
        }
    }

    @Override
    public Optional<AuditLog> findByEventId(String eventId) {
        try {
            GetRequest getRequest = new GetRequest(INDEX, eventId);
            GetResponse response = elasticClient.get(getRequest, RequestOptions.DEFAULT);
            if(response.isExists()) {
                return Optional.of(mapper.readValue(response.getSourceAsString(), AuditLog.class));
            }
            return Optional.empty();
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve audit log by ID:", e);
        }
    }

    @Override
    public List<AuditLog> advancedSearch(AuditSearchRequest request) {

        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        if (userOpt.isEmpty()) {
            throw new AccessDeniedException("Unauthenticated request");
        }
        UserContext user = userOpt.get();

        if(request.getChangedByUserId() != null &&
                !user.isAdmin() &&
                !Objects.equals(request.getChangedByUserId(), user.getUserId())) {
            // non admin user can only access audit logs for his own userId.
            // which means for non-admin user the, the userId in the request parameter is mandatory and should match
            // with the userId of the logged user.
            throw new AccessDeniedException("Unauthenticated request");
        }

        try {
            BoolQueryBuilder query = QueryBuilders.boolQuery();

            if(request.getEntityId() != null) {
                query.must(QueryBuilders.matchQuery("entityType", request.getEntityType()));
            }
            if(request.getEntityId() != null) {
                query.must(QueryBuilders.matchQuery("entityId", request.getEntityId()));
            }
            if(request.getEventType() != null) {
                query.must(QueryBuilders.matchQuery("eventType", request.getEventType()));
            }
            if(request.getSourceService() != null) {
                query.must(QueryBuilders.matchQuery("sourceService", request.getSourceService()));
            }
            if(request.getChangedByUserId() != null) {
                query.must(QueryBuilders.matchQuery("changedBy.userId", request.getChangedByUserId()));
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
            throw new RuntimeException("Failed to perform advanced search", e);
        }
    }
}
