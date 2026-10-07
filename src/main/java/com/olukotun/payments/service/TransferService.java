package com.olukotun.payments.service;

import com.olukotun.payments.dto.TransferRequest;
import com.olukotun.payments.dto.TransferResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransferService {
    public TransferResponse createTransfer(TransferRequest request) {
        return new TransferResponse(
                UUID.randomUUID(),
                "ACCEPTED",
                request.amount(),
                request.currency()
        );
    }
}
