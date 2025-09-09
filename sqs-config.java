package com.example.sqslistener.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.AcknowledgementOrdering;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.InstanceProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
@Configuration
public class SqsConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        log.info("Configuring SQS client with IAM role in region: {}", awsRegion);
        
        return SqsAsyncClient.builder()
                .region(Region.of(awsRegion))
                // DefaultCredentialsProvider automatically uses IAM role when running on EC2/ECS/Lambda
                // It follows this order:
                // 1. Environment variables
                // 2. System properties
                // 3. Web Identity Token (EKS)
                // 4. Instance profile credentials (EC2/ECS IAM role)
                // 5. Container credentials (ECS task role)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {
        return SqsMessageListenerContainerFactory
                .builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options
                        .acknowledgementMode(AcknowledgementMode.MANUAL)
                        .acknowledgementInterval(Duration.ofSeconds(3))
                        .acknowledgementThreshold(5)
                        .acknowledgementOrdering(AcknowledgementOrdering.ORDERED)
                        .maxConcurrentMessages(10)
                        .pollTimeout(Duration.ofSeconds(20))
                        .maxMessagesPerPoll(10)
                        .messageVisibilityTimeout(Duration.ofSeconds(30))
                )
                .build();
    }
}