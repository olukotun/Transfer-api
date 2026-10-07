package com.olukotun.payments.service;

import com.olukotun.payments.dto.TransferRequest;
import com.olukotun.payments.dto.TransferResponse;
import com.olukotun.payments.entity.Transfer;
import com.olukotun.payments.exception.IdempotencyConflictException;
import com.olukotun.payments.exception.InvalidTransferException;
import com.olukotun.payments.exception.TransferNotFoundException;
import com.olukotun.payments.respository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;


@Service
public class TransferService {
    private final TransferRepository transferRepository;

    public TransferService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Transactional
    public TransferResponse createTransfer(String idempotencyKey, TransferRequest request) {
        System.out.println("DEBUG service key: " + idempotencyKey);
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new InvalidTransferException(
                    "Idempotency key must contain 1 to 100 characters"
            );
        }

        if (request.sourceAccount().equals(request.destinationAccount())) {
            throw new InvalidTransferException(
                    "Source and destination accounts must be different"
            );
        }

        Optional<Transfer> existing = transferRepository.findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            Transfer transfer = existing.get();
            System.out.println("DEBUG existing transfer found: " + true);
            boolean sameDetails =
                    transfer.getSourceAccount().equals(request.sourceAccount())
                            && transfer.getDestinationAccount().equals(request.destinationAccount())
                            && transfer.getAmount().compareTo(request.amount()) == 0
                            && transfer.getCurrency().equals(request.currency());

            if (!sameDetails) {throw new IdempotencyConflictException();}

            return toResponse(transfer);
        }

        Transfer transfer = new Transfer(
                idempotencyKey,
                request.sourceAccount(),
                request.destinationAccount(),
                request.amount(),
                request.currency()
        );
        System.out.println("DEBUG entity key: " + transfer.getIdempotencyKey());

        return toResponse(transferRepository.save(transfer));
    }

    private TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getStatus(),
                transfer.getAmount(),
                transfer.getCurrency()
        );
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransfer(UUID id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new TransferNotFoundException(id));

        return new TransferResponse(
                transfer.getId(),
                transfer.getStatus(),
                transfer.getAmount(),
                transfer.getCurrency()
        );
    }
}
