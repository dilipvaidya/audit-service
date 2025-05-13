package com.dilip.audit_service.data.repository;

import com.amazonaws.services.s3.AmazonS3;
import com.dilip.audit_service.data.entity.AuditLog;
import com.dilip.audit_service.data.entity.AuditSearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Repository("s3")
@Profile({"dev", "prod"})
public class AuditRepositoryAwsS3Impl implements AuditRepository {

    private final AmazonS3 amazonS3;
    private final ObjectMapper mapper;

    @Value("${db.coldStorage.bucketName}")
    private String bucketName;


    @Autowired
    public AuditRepositoryAwsS3Impl(AmazonS3 s3, ObjectMapper mapper) {
        this.amazonS3 = s3;
        this.mapper = mapper;
    }


    @Override
    public void save(AuditLog auditLog) {
        try {
            String key = "logs/" + auditLog.getEventId() + ".json";
            String json = mapper.writeValueAsString(auditLog);
            amazonS3.putObject(bucketName, key, json); //TODO: assumption is bucket already exists.
        } catch (Exception e) {
            throw new RuntimeException("Failed to save audit log to S3", e);
        }
    }

    @Override
    public List<AuditLog> findAll() {
        try {
            List<AuditLog> logs = new ArrayList<>();
            var objectListing = amazonS3.listObjects(bucketName, "logs/");
            var summaries = objectListing.getObjectSummaries();
            for (var summary : summaries) {
                String content = new String(amazonS3.getObject(bucketName, summary.getKey()).getObjectContent().readAllBytes());
                logs.add(mapper.readValue(content, AuditLog.class));
            }
            return logs;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch audit logs from S3", e);
        }
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        return List.of();
    }

    @Override
    public Optional<AuditLog> findByEventId(String eventId) {
        try {
            String key = "logs/" + eventId + ".json";
            if (!amazonS3.doesObjectExist(bucketName, key)) {
                return Optional.empty();
            }
            String content = new String(amazonS3.getObject(bucketName, key).getObjectContent().readAllBytes());
            return Optional.of(mapper.readValue(content, AuditLog.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch audit log from S3", e);
        }
    }

    @Override
    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        // For basic S3 setup, advanced filtering would require loading and filtering in-memory.
        return findAll().stream()
                .filter(log -> request.getEntityType() == null || request.getEntityType().equals(log.getEntityType()))
                .filter(log -> request.getEntityId() == null || request.getEntityId().equals(log.getEntityId()))
                .filter(log -> request.getEventType() == null || request.getEventType().equals(log.getEventType()))
                .filter(log -> request.getSourceService() == null || request.getSourceService().equals(log.getSourceService()))
                .filter(log -> request.getChangedByUserId() == null ||
                        (log.getChangedBy() != null && request.getChangedByUserId().equals(log.getChangedBy().getUserId())))
                .toList();
    }
}


/*
@Repository("s3")
@Profile({"dev", "prod", "beta"})
class AuditRepositoryAwsS3Impl implements AuditRepository {

    private final AmazonS3 amazonS3;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BUCKET_NAME = "audit-logs";

    @Autowired
    public AuditRepositoryAwsS3Impl(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    @Override
    public void save(AuditLog auditLog) {
        try {
            String key = auditLog.getEventId() + ".json.gz";
            byte[] content = objectMapper.writeValueAsBytes(auditLog);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(byteStream)) {
                gzipOut.write(content);
            }

            byte[] compressedContent = byteStream.toByteArray();
            ByteArrayInputStream inputStream = new ByteArrayInputStream(compressedContent);
            amazonS3.putObject(new PutObjectRequest(BUCKET_NAME, key, inputStream, null));
        } catch (Exception e) {
            throw new RuntimeException("Failed to save audit log to S3", e);
        }
    }

    @Override
    public List<AuditLog> findAll() {
        try {
            ObjectListing objectListing = amazonS3.listObjects(BUCKET_NAME);
            List<AuditLog> results = new ArrayList<>();

            for (S3ObjectSummary summary : objectListing.getObjectSummaries()) {
                S3Object s3Object = amazonS3.getObject(BUCKET_NAME, summary.getKey());
                try (GZIPInputStream gzipIn = new GZIPInputStream(s3Object.getObjectContent())) {
                    AuditLog log = objectMapper.readValue(gzipIn, AuditLog.class);
                    results.add(log);
                }
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve logs from S3", e);
        }
    }

    @Override
    public Optional<AuditLog> findById(String eventId) {
        try {
            String key = eventId + ".json.gz";
            if (!amazonS3.doesObjectExist(BUCKET_NAME, key)) {
                return Optional.empty();
            }
            S3Object s3Object = amazonS3.getObject(BUCKET_NAME, key);
            try (GZIPInputStream gzipIn = new GZIPInputStream(s3Object.getObjectContent())) {
                AuditLog log = objectMapper.readValue(gzipIn, AuditLog.class);
                return Optional.of(log);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve log from S3", e);
        }
    }

    @Override
    public List<AuditLog> advancedSearch(AuditSearchRequest request) {
        return findAll().stream()
                .filter(log -> request.getEntityType() == null || request.getEntityType().equals(log.getEntityType()))
                .filter(log -> request.getEntityId() == null || request.getEntityId().equals(log.getEntityId()))
                .filter(log -> request.getEventType() == null || request.getEventType().equals(log.getEventType()))
                .filter(log -> request.getSourceService() == null || request.getSourceService().equals(log.getSourceService()))
                .filter(log -> request.getChangedByUserId() == null ||
                        (log.getChangedBy() != null && request.getChangedByUserId().equals(log.getChangedBy().getUserId())))
                .filter(log -> {
                    try {
                        if (request.getStartTime() != null && Instant.parse(request.getStartTime()).isAfter(log.getTimestamp())) return false;
                        if (request.getEndTime() != null && Instant.parse(request.getEndTime()).isBefore(log.getTimestamp())) return false;
                    } catch (Exception ignored) {}
                    return true;
                })
                .skip((long) request.getPage() * request.getSize())
                .limit(request.getSize())
                .collect(Collectors.toList());
    }
}
 */