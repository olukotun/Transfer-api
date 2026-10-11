package com.olukotun.payments.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payments.worker.enabled", havingValue = "true")
public class TransferWorker {

    private final TransferProcessor transferProcessor;

    public TransferWorker(TransferProcessor transferProcessor) {
        this.transferProcessor = transferProcessor;
    }

    @Scheduled(
            fixedDelayString = "${payments.worker.poll-delay-ms:2000}"
    )
    public void processPendingTransfer() {
        transferProcessor.processNextPendingTransfer();
    }
}