package com.dilip.audit_service.data.entity;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Data
@Getter
@Setter
@ToString
public class AuditSearchRequest {
    private String startTime;
    private String endTime;
    private String entityType;
    private String entityId;
    private String eventType;
    private String sourceService;
    private String changedByUserId;
    private List<String> roles;
    private int page = 0;
    private int size = 10;
    private String sort;
}