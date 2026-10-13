package com.olukotun.payments.service;

import com.olukotun.payments.respository.TransferOutboxRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Duration;

@Service
@ConditionalOnProperty(name = "payments.sqs.enabled", havingValue = "true")
public class TransferOutboxPublisher {

    private final TransferOutboxRepository outboxRepository;
    private final SqsClient sqsClient;
    private final String queueUrl;

    public TransferOutboxPublisher(TransferOutboxRepository outboxRepository, SqsClient sqsClient, @Value("${payments.sqs.queue-url}") String queueUrl) {

        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalArgumentException("payments.sqs.queue-url must be configured");
        }

        this.outboxRepository = outboxRepository;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Transactional
    public boolean publishNextEvent() {
        var pending = outboxRepository.findNextUnpublishedForUpdate();

        if (pending.isEmpty()) {
            return false;
        }

        var event = pending.get();

        // UUID values are safe to insert into this fixed JSON structure.
        String body = """
                {
                  "eventId": "%s",
                  "transferId": "%s",
                  "eventType": "TRANSFER_REQUESTED"
                }
                """.formatted(event.getId(), event.getTransferId());

        sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(10)).apiCallAttemptTimeout(Duration.ofSeconds(5))).build());

        event.markPublished();

        return true;
    }
}