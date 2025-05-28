package com.dilip.audit_service.health;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuditServiceHealthIndicator implements HealthIndicator {
    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Override
    public Health health() {
        // Perform custom health checks (e.g., DB, Kafka, MinIO)
        boolean isKafkaHealthy = checkKafkaConnection();
        boolean isElasticsearchHealthy = checkElasticsearch();
        boolean isAWSS3Healthy = checkAWSW3();
        boolean isMinIOHealthy = checkMinIO();

        if (isKafkaHealthy && isElasticsearchHealthy && isAWSS3Healthy && isMinIOHealthy) {
            return Health.up()
                    .withDetail("kafka", "Available")
                    .withDetail("elasticsearch", "Available")
                    .withDetail("awss3", "Available")
                    .withDetail("minio", "Available")
                    .build();
        } else {
            return Health.down()
//                    .withDetail("kafka", isKafkaHealthy ? "OK" : "Unreachable")
//                    .withDetail("elasticsearch", isElasticsearchHealthy ? "OK" : "Unreachable")
//                    .withDetail("awss3", isAWSS3Healthy ? "OK" : "Unreachable")
                    .withDetails(Map.of(
                        "kafka", isKafkaHealthy ? "OK" : "Unreachable",
                        "elasticsearch", isElasticsearchHealthy ? "OK" : "Unreachable",
                        "awss3", isAWSS3Healthy ? "OK" : "Unreachable",
                        "minio", isMinIOHealthy ? "OK" : "Unreachable"
                    ))
                    .build();
        }
    }

    private boolean checkKafkaConnection() {
        // todo: Kafka health check logic
        return true; // Replace with real check
    }

    private boolean checkElasticsearch() {
        // todo: Elasticsearch health check logic
        try {
            return elasticsearchClient.ping().value();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkAWSW3() {
        // todo: AWS S3 health check logic here
        return true; // Replace with real check
    }

    private boolean checkMinIO() {
        // todo: checkMinIO health check logic here
        // todo: this check should only happen in dev environment as its not present in the prod.
        return true; // Replace with real check
    }
}
