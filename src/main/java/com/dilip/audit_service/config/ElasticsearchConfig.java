package com.dilip.audit_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "elasticsearch") // Better for bulk property binding
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    private String host = "localhost"; //sensible default
    private int port = 9200;
    private String username;
    private String password;
    private boolean sslEnabled = false;
    private long connectTimeout = 50000;
    private long socketTimeout = 60000;

    @Override
    public ClientConfiguration clientConfiguration() {
        System.out.printf("ElasticsearchConfig:> %s, %d\n", this.host, this.port);
        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
                ClientConfiguration.builder()
                .connectedTo("%s:%d".formatted(this.host, this.port));

        if (username != null && !username.isBlank()) {
            builder.withBasicAuth(username, password);
        }

        // SSL Configuration
        if (sslEnabled) {
            builder.usingSsl(); // Trust all for dev (configure properly for prod)
        }

        return builder
                .withConnectTimeout(connectTimeout) // Custom timeouts
                .withSocketTimeout(socketTimeout)
                .build();
    }
}

