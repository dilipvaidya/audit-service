package com.dilip.audit_service.data.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuditRepositoryFactory {

    @Value("${db.transient.type}")
    private String transientDb;

    @Value("${db.coldStorage.type}")
    private String coldStorageDb;

    public AuditRepository getTransientRepository() {
        if ("elasticsearch".equalsIgnoreCase(transientDb)) {
            return new AuditRepositoryElasticImpl();
        } else if ("mongodb".equalsIgnoreCase(transientDb)) {
            return null;
        }

        throw new IllegalArgumentException("Unknown transient database: " + transientDb);
    }

    public AuditRepository getColdStorageRepository() {
        if ("s3".equalsIgnoreCase(coldStorageDb)) {
            return new AuditRepositoryElasticImpl();
        } else if ("postgres".equalsIgnoreCase(coldStorageDb)) {
            return null;
        }

        throw new IllegalArgumentException("Unknown cold-storage: " + coldStorageDb);
    }
}
