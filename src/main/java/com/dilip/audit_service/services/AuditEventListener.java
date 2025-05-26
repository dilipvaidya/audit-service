package com.dilip.audit_service.services;

import com.dilip.audit_service.data.entity.AuditLog;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class AuditEventListener {
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public AuditEventListener(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "audit-events", groupId = "audit-service")
    public void handleAuditEvent(String message) {
        try {
            AuditLog auditLog = objectMapper.readValue(message, AuditLog.class);
            auditService.saveAuditLog(auditLog);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
