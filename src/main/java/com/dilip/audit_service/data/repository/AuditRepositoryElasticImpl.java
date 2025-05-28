package com.dilip.audit_service.data.repository;

import com.dilip.audit_service.common.DeletionResult;
import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.dilip.audit_service.data.entity.UserContext;
import com.dilip.audit_service.exception.NotFoundException;
import com.dilip.audit_service.services.UserContextService;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository("elasticsearch")
@Profile({"dev", "prod"})
public class AuditRepositoryElasticImpl implements AuditRepository {

    private static final Logger log = LoggerFactory.getLogger(AuditRepositoryElasticImpl.class);
    private final ElasticsearchOperations elasticsearchOperations;
    private final UserContextService userContextService;
    private static final String INDEX = "audit-logs";

    @Autowired
    public AuditRepositoryElasticImpl(ElasticsearchOperations elasticsearchOperations,
                                      UserContextService userContextService) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.userContextService = userContextService;
    }

    @Override
    public void save(AuditLog auditLog) {
        IndexQuery indexQuery = new IndexQueryBuilder()
                .withId(auditLog.getEventId())
                .withObject(auditLog)
                .build();
        elasticsearchOperations.index(indexQuery, IndexCoordinates.of(INDEX));
    }

    @Override
    public List<AuditLog> findAll() {
        return advancedSearch(new AuditSearchRequest());
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        AuditSearchRequest request = new AuditSearchRequest();
        request.setChangedByUserId(userId);
        List<AuditLog> auditLogs = advancedSearch(request);

        if (!auditLogs.isEmpty() && !userId.equals(auditLogs.get(0).getChangedBy().getUserId())) {
            throw new NotFoundException();
        }
        return auditLogs;
    }

    @Override
    public Optional<AuditLog> findByEventId(String eventId) {
        AuditSearchRequest request = new AuditSearchRequest();
        request.setEventId(eventId);
        List<AuditLog> auditLogs = advancedSearch(request);

        if (!auditLogs.isEmpty() && !eventId.equals(auditLogs.get(0).getEventId())) {
            throw new NotFoundException();
        }
        return auditLogs.isEmpty() ? Optional.empty() : Optional.of(auditLogs.get(0));
    }

    @Override
    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        // Authorization logic remains the same
        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        UserContext user = userOpt.orElseThrow(() ->
                new AccessDeniedException("failed to perform advanced search: unauthenticated request")
        );

        if (request.getChangedByUserId() != null &&
                !user.isAdmin() &&
                !Objects.equals(request.getChangedByUserId(), user.getUserId())) {
            throw new AccessDeniedException("failed to perform advanced search: unauthorised request");
        }

        return search(request);
    }

    private List<AuditLog> search(AuditSearchRequest request) {
        Criteria criteria = new Criteria();

        // Combine multiple criteria as needed:
        if (request.getEventId() != null) {
            criteria = criteria.and("eventId").is(request.getEventId());
        }
        if (request.getEventType() != null) {
            criteria = criteria.and("eventType").is(request.getEventType());
        }

        if (request.getEntityType() != null) {
            criteria = criteria.and("entityId").is(request.getEntityId());
        }
        if (request.getEventType() != null) {
            criteria = criteria.and("entityType").is(request.getEntityType());
        }

        if (request.getSourceService() != null) {
            criteria = criteria.and("sourceService").is(request.getSourceService());
        }

        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        if(userOpt.isPresent()) {
            UserContext user = userOpt.get();
            if (user.isAdmin() && request.getChangedByUserId() != null) {
                criteria = criteria.and("changedBy.userId").is(request.getChangedByUserId());
            }
            if (!user.isAdmin()) {
                criteria = criteria.and("changedBy.userId").is(user.getUserId());
            }
        }

        CriteriaQuery searchQuery = new CriteriaQuery(
                criteria, PageRequest.of(request.getPage(), request.getSize()));

        SearchHits<AuditLog> hits = elasticsearchOperations.search
                (searchQuery, AuditLog.class, IndexCoordinates.of(INDEX));

        return hits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }

    @Override
    public DeletionResult deleteEvent(String eventId) {
        // Authorization logic remains the same
        Optional<UserContext> userOpt = userContextService.getCurrentUser();
        if (userOpt.isEmpty()) {
            throw new AccessDeniedException("could not delete from transient database: Unauthenticated request");
        }

        UserContext user = userOpt.get();
        if (!user.isAdmin()) {
            AuditLog auditLog = findByEventId(eventId)
                    .orElseThrow(() -> new AccessDeniedException("Unauthenticated request"));

            if (!auditLog.getChangedBy().getUserId().equals(user.getUserId())) {
                throw new AccessDeniedException("could not delete from transient database: Unauthenticated request");
            }
        }

        // New deletion approach
//        StringQuery deleteQuery = new StringQuery(
//                "{\"term\":{\"eventId\":{\"value\":\"" + eventId + "\"}}}"
//        );
        Criteria criteria = new Criteria("eventId").is(eventId);
        Query deleteQuery = new CriteriaQuery(criteria);

        ByQueryResponse deleted = elasticsearchOperations.delete(
                deleteQuery, AuditLog.class, IndexCoordinates.of(INDEX));

        if (deleted.getDeleted() > 0) {
            return new DeletionResult.DeletionSuccess(deleted.getDeleted());
        } else {
            return new DeletionResult.DeletionNotFound("could not delete from transient database: Not Found");
        }


        /*
         Unhandled cases:
        2025-05-28T20:48:20.861Z DEBUG 98 --- [audit-service] [nio-8080-exec-1] o.s.security.web.FilterChainProxy        : Securing DELETE /api/audit/logs/event/12345

        2025-05-28T20:48:20.890Z DEBUG 98 --- [audit-service] [nio-8080-exec-1] o.s.s.a.dao.DaoAuthenticationProvider    : Authenticated user

        2025-05-28T20:48:20.890Z DEBUG 98 --- [audit-service] [nio-8080-exec-1] o.s.s.w.a.www.BasicAuthenticationFilter  : Set SecurityContextHolder to UsernamePasswordAuthenticationToken [Principal=com.dilip.audit_service.config.CustomUserDetails [Username=user-001, Password=[PROTECTED], Enabled=true, AccountNonExpired=true, CredentialsNonExpired=true, AccountNonLocked=true, Granted Authorities=[ROLE_USER]], Credentials=[PROTECTED], Authenticated=true, Details=WebAuthenticationDetails [RemoteIpAddress=192.168.65.1, SessionId=null], Granted Authorities=[ROLE_USER]]

        2025-05-28T20:48:20.894Z DEBUG 98 --- [audit-service] [nio-8080-exec-1] o.s.security.web.FilterChainProxy        : Secured DELETE /api/audit/logs/event/12345

        2025-05-28T20:48:20.895Z DEBUG 98 --- [audit-service] [nio-8080-exec-1] o.s.web.servlet.DispatcherServlet        : DELETE "/audit-service/api/audit/logs/event/12345", parameters={}

        2025-05-28T20:47:39.692Z DEBUG 98 --- [audit-service] [io-8080-exec-10] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped to com.dilip.audit_service.controller.AuditController#deleteEvent(String)

        2025-05-28T20:47:39.784Z DEBUG 98 --- [audit-service] [io-8080-exec-10] org.elasticsearch.client.RestClient      : request [POST http://elasticsearch:9200/audit-logs/_delete_by_query]⁠ returned [HTTP/1.1 400 Bad Request]

        2025-05-28T20:47:39.819Z DEBUG 98 --- [audit-service] [io-8080-exec-10] o.s.web.servlet.DispatcherServlet        : Failed to complete request: org.springframework.data.elasticsearch.UncategorizedElasticsearchException: [es/delete_by_query] failed: [parsing_exception] unknown query [query]

        2025-05-28T20:47:39.820Z ERROR 98 --- [audit-service] [io-8080-exec-10] o.s.b.w.servlet.support.ErrorPageFilter  : Forwarding to error page from request [/api/audit/logs/event/5463] due to exception [[es/delete_by_query] failed: [parsing_exception] unknown query [query]]


        org.springframework.data.elasticsearch.UncategorizedElasticsearchException: [es/delete_by_query] failed: [parsing_exception] unknown query [query]




        2025-05-28T20:49:57.691Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] o.s.security.web.FilterChainProxy        : Securing DELETE /api/audit/logs/event/12345

        2025-05-28T20:49:57.693Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] o.s.s.a.dao.DaoAuthenticationProvider    : Authenticated user

        2025-05-28T20:49:57.693Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] o.s.s.w.a.www.BasicAuthenticationFilter  : Set SecurityContextHolder to UsernamePasswordAuthenticationToken [Principal=com.dilip.audit_service.config.CustomUserDetails [Username=admin, Password=[PROTECTED], Enabled=true, AccountNonExpired=true, CredentialsNonExpired=true, AccountNonLocked=true, Granted Authorities=[ROLE_ADMIN]], Credentials=[PROTECTED], Authenticated=true, Details=WebAuthenticationDetails [RemoteIpAddress=192.168.65.1, SessionId=null], Granted Authorities=[ROLE_ADMIN]]

        2025-05-28T20:49:57.695Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] o.s.security.web.FilterChainProxy        : Secured DELETE /api/audit/logs/event/12345

        2025-05-28T20:49:57.696Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] o.s.web.servlet.DispatcherServlet        : DELETE "/audit-service/api/audit/logs/event/12345", parameters={}

        2025-05-28T20:49:57.697Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] s.w.s.m.m.a.RequestMappingHandlerMapping : Mapped to com.dilip.audit_service.controller.AuditController#deleteEvent(String)

        2025-05-28T20:49:57.715Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] org.elasticsearch.client.RestClient      : request [POST http://elasticsearch:9200/audit-logs/_delete_by_query]⁠ returned [HTTP/1.1 400 Bad Request]

        2025-05-28T20:49:57.719Z DEBUG 98 --- [audit-service] [nio-8080-exec-4] o.s.web.servlet.DispatcherServlet        : Failed to complete request: org.springframework.data.elasticsearch.UncategorizedElasticsearchException: [es/delete_by_query] failed: [parsing_exception] unknown query [query]

        2025-05-28T20:49:57.722Z ERROR 98 --- [audit-service] [nio-8080-exec-4] o.s.b.w.servlet.support.ErrorPageFilter  : Forwarding to error page from request [/api/audit/logs/event/12345] due to exception [[es/delete_by_query] failed: [parsing_exception] unknown query [query]]


        org.springframework.data.elasticsearch.UncategorizedElasticsearchException: [es/delete_by_query] failed: [parsing_exception] unknown query [query]


        Caused by: co.elastic.clients.elasticsearch._types.ElasticsearchException: [es/delete_by_query] failed: [parsing_exception] unknown query [query]

        at co.elastic.clients.transport.ElasticsearchTransportBase.getApiResponse(ElasticsearchTransportBase.java:345) ~[elasticsearch-java-8.15.5.jar:na]

        at co.elastic.clients.transport.ElasticsearchTransportBase.performRequest(ElasticsearchTransportBase.java:147) ~[elasticsearch-java-8.15.5.jar:na]

        at co.elastic.clients.elasticsearch.ElasticsearchClient.deleteByQuery(ElasticsearchClient.java:617) ~[elasticsearch-java-8.15.5.jar:na]

        at org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate.lambda$delete$3(ElasticsearchTemplate.java:192) ~[spring-data-elasticsearch-5.4.5.jar:5.4.5]

        at org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate.execute(ElasticsearchTemplate.java:702) ~[spring-data-elasticsearch-5.4.5.jar:5.4.5]

         */
    }
}