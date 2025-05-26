package com.dilip.audit_service.controller;

import com.dilip.audit_service.common.DeletionResult;
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

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostMapping("/logs")
    public ResponseEntity<Void> writeAuditLog(@RequestBody AuditLog auditLog) {
        auditService.saveAuditLog(auditLog);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditService.queryAuditLogs();
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

    @GetMapping("/query")
    public ResponseEntity<List<AuditLog>> advancedSearch(@RequestBody AuditSearchRequest request) {
        List<AuditLog> results = auditService.advancedSearch(request);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/logs/event/{eventId}")
    public ResponseEntity<?> deleteEvent(@PathVariable String eventId) {

        DeletionResult deletionResult = auditService.deleteEvent(eventId);
        if(deletionResult instanceof DeletionResult.DeletionNotFound) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(((DeletionResult.DeletionNotFound) deletionResult).message());
        } else if(deletionResult instanceof DeletionResult.DeletionFailure) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(((DeletionResult.DeletionFailure) deletionResult).reason()); // 500
        }

        return ResponseEntity.ok("deleted");
    }
}
