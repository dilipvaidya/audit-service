package com.dilip.audit_service.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "s3")
public class AmazonS3Config {

    private String endpoint;
    private String region;
    private String accessKey;
    private String secretKey;
    private boolean pathStyleAccess = true; // Default for MinIO compatibility

    @Bean
    public AmazonS3 amazonS3() {
        return AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        this.endpoint, this.region))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(this.accessKey, this.secretKey)))
                .withPathStyleAccessEnabled(true)
                .build();
    }
}
