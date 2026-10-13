package com.olukotun.payments.service;

import com.olukotun.payments.respository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.olukotun.payments.exception.TransferNotFoundException;
import java.util.UUID;

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

    @Transactional
    public boolean processTransfer(UUID transferId) {
        var transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> new TransferNotFoundException(transferId));

        if ("COMPLETED".equals(transfer.getStatus())) {
            // Duplicate delivery: this transfer is already finished.
            return false;
        }

        if (!"PENDING".equals(transfer.getStatus())) {
            throw new IllegalStateException(
                    "Cannot process transfer with status: "
                            + transfer.getStatus()
            );
        }

        // Simulated processing only; no money movement.
        transfer.complete();

        return true;
    }

}