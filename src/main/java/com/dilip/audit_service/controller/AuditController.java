package com.dilip.audit_service.controller;

import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.dilip.audit_service.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    @Autowired
    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/logs")
    public ResponseEntity<Void> writeAuditLog(@RequestBody AuditLog auditLog) {
        auditService.saveAuditLog(auditLog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam Optional<String> startTime,
            @RequestParam Optional<String> endTime,
            @RequestParam Optional<String> entityType,
            @RequestParam Optional<String> entityId,
            @RequestParam Optional<String> eventType,
            @RequestParam Optional<String> sourceService,
            @RequestParam Optional<String> changedByUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam Optional<String> sort
    ) {
        List<AuditLog> logs = auditService.queryAuditLogs(
                startTime,
                endTime,
                entityType,
                entityId,
                eventType,
                sourceService,
                changedByUserId,
                page,
                size,
                sort);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<AuditLog>> getAuditLogByUserId(@PathVariable String userId) {
        List<AuditLog> results = auditService.findByUserId(userId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/logs/event/{eventId}")
    public ResponseEntity<AuditLog> getAuditLogByEventId(@PathVariable String eventId) {
        return auditService.findByEventId(eventId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/query")
    public ResponseEntity<List<AuditLog>> advancedSearch(@RequestBody AuditSearchRequest request) {
        List<AuditLog> results = auditService.advancedSearch(request);
        return ResponseEntity.ok(results);
    }
}
