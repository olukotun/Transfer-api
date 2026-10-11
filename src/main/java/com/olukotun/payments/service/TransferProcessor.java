package com.olukotun.payments.service;

import com.olukotun.payments.respository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferProcessor {

    private final TransferRepository transferRepository;

    public TransferProcessor(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Transactional
    public boolean processNextPendingTransfer() {
        var pending = transferRepository.findNextPendingForUpdate();

        if (pending.isEmpty()) {
            return false;
        }

        var transfer = pending.get();

        // Simulated processing only; no money movement.
        transfer.complete();

        return true;
    }
}