package com.olukotun.payments.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payments.sqs.enabled", havingValue = "true")
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final TransferOutboxPublisher publisher;

    public OutboxPublisherScheduler(TransferOutboxPublisher publisher) {
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${payments.outbox.poll-delay-ms:2000}")
    public void publish() {
        try {
            if (publisher.publishNextEvent()) {
                log.info("Outbox event published to SQS");
            }
        } catch (Exception exception) {
            log.error("Outbox publication failed; event remains available for retry", exception);
        }
    }
}