package com.dilip.audit_service.data.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AuditRepositoryFactory {

    private static final Logger logger = LoggerFactory.getLogger(AuditRepositoryFactory.class);

    @Value("${db.transient.type}")
    private String transientDb;

    @Value("${db.coldStorage.type}")
    private String coldStorageDb;

    private final Map<String, AuditRepository> repositoryMap;

    @Autowired
    public AuditRepositoryFactory(List<AuditRepository> auditRepositories) {
        this.repositoryMap = new HashMap<>();
        for (AuditRepository auditRepo : auditRepositories) {
            Class<?> targetClass = org.springframework.aop.support.AopUtils.getTargetClass(auditRepo);
            Repository annotation = targetClass.getAnnotation(Repository.class);
            if(annotation != null && !annotation.value().isEmpty()) {
                this.repositoryMap.put(annotation.value(), auditRepo);
            }
        }
        logger.info("Loaded audit repositories: {}", repositoryMap.keySet());
    }

    public AuditRepository getTransientRepository() {
        AuditRepository repo = repositoryMap.get(transientDb);
        if (repo == null) {
            logger.error("Transient repository '{}' not available in current profile.", transientDb);
            throw new IllegalArgumentException("Transient repository not available: " + transientDb);
        }
        return repo;
    }

    public AuditRepository getColdStorageRepository() {
        AuditRepository auditRepo = repositoryMap.get(coldStorageDb);
        if(auditRepo == null) {
            logger.error("cold-storage repository '{}' not available in current profile.", coldStorageDb);
            throw new IllegalArgumentException("cold-storage repository not available: " + coldStorageDb);
        }
        return auditRepo;
    }
}
