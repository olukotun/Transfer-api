package com.olukotun.payments.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@ConditionalOnProperty(
        name = "payments.sqs.enabled",
        havingValue = "true"
)
public class SqsConfig {

    @Bean(destroyMethod = "close")
    public SqsClient sqsClient(
            @Value("${payments.sqs.region}") String region) {

        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }
}