package com.dilip.audit_service.data.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Getter
@Setter
@ToString
public class AuditLog {
    private String eventId;
    private Instant timestamp;
    private String sourceService;
    private String eventType;
    private String entityType;
    private String entityId;
    private ChangedBy changedBy;
    private Map<String, ChangeDetail> changeSummary;
    private Metadata metadata;

    @Data
    public static class ChangedBy {
        private String username;
        private String userId;
        private List<String> roles;
    }

    @Data
    public static class ChangeDetail {
        private String oldValue;
        private String newValue;
    }

    @Data
    public static class Metadata {
        private String ipAddress;
        private String userAgent;
    }
}
