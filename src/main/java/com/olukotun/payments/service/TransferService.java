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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;


@Service
public class TransferService {
    private final TransferRepository transferRepository;
    private final TransactionTemplate transactionTemplate;

    public TransferService(
            TransferRepository transferRepository,
            PlatformTransactionManager transactionManager) {

        this.transferRepository = transferRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }


    private TransferResponse createTransferInTransaction(String idempotencyKey, TransferRequest request) {
        System.out.println("DEBUG service key: " + idempotencyKey);
        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            throw new InvalidTransferException("Idempotency key must contain 1 to 100 characters");
        }

        if (request.sourceAccount().equals(request.destinationAccount())) {
            throw new InvalidTransferException("Source and destination accounts must be different");
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

            if (!sameDetails) {
                throw new IdempotencyConflictException();
            }

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

    public TransferResponse createTransfer(String idempotencyKey, TransferRequest request) {

        try {
            return transactionTemplate.execute(transaction ->
                    createTransferInTransaction(idempotencyKey, request)
            );
        } catch (DataIntegrityViolationException exception) {
            if (!isIdempotencyKeyViolation(exception)) {
                throw exception;
            }

            // The failed transaction has rolled back.
            // In a fresh transaction, find and compare the winning transfer.
            return transactionTemplate.execute(transaction ->
                    createTransferInTransaction(idempotencyKey, request)
            );
        }
    }

    private boolean isIdempotencyKeyViolation(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException violation) {
                return "uk_transfers_idempotency_key".equals(violation.getConstraintName());
            }
            current = current.getCause();
        }

        return false;
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
