package com.olukotun.payments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = {"payments.sqs.enabled", "payments.sqs.consumer-enabled"}, havingValue = "true")
public class TransferSqsConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferSqsConsumer.class);

    private final SqsClient sqsClient;
    private final TransferProcessor processor;
    private final JsonMapper jsonMapper;
    private final String queueUrl;

    public TransferSqsConsumer(SqsClient sqsClient, TransferProcessor processor, JsonMapper jsonMapper, @Value("${payments.sqs.queue-url}") String queueUrl) {

        this.sqsClient = sqsClient;
        this.processor = processor;
        this.jsonMapper = jsonMapper;
        this.queueUrl = queueUrl;
    }

    @Scheduled(fixedDelay = 1000)
    public void consume() {
        try {
            var response = sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(1).waitTimeSeconds(10).visibilityTimeout(60).overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(25)).apiCallAttemptTimeout(Duration.ofSeconds(15))).build());

            for (var message : response.messages()) {
                try {
                    Map<?, ?> body = jsonMapper.readValue(message.body(), Map.class);

                    if (!"TRANSFER_REQUESTED".equals(body.get("eventType"))) {
                        throw new IllegalArgumentException("Unsupported event type");
                    }

                    if (!(body.get("transferId") instanceof String id)) {
                        throw new IllegalArgumentException("Missing or invalid transferId");
                    }

                    if (!(body.get("eventId") instanceof String eventId)) {
                        throw new IllegalArgumentException("Missing or invalid eventId");
                    }

                    UUID.fromString(eventId);
                    UUID transferId = UUID.fromString(id);

                    // This separate bean commits before returning.
                    boolean completed = processor.processTransfer(transferId);

                    sqsClient.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(10)).apiCallAttemptTimeout(Duration.ofSeconds(5))).build());

                    log.info("SQS message acknowledged: transferId={}, newlyCompleted={}", transferId, completed);
                } catch (Exception exception) {
                    log.error("Message {} failed; left in SQS for retry", message.messageId(), exception);
                }
            }
        } catch (Exception exception) {
            log.error("SQS receive failed; will retry on next poll", exception);
        }
    }
}