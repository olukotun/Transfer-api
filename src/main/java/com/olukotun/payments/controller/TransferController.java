package com.olukotun.payments.controller;

import com.olukotun.payments.dto.TransferRequest;
import com.olukotun.payments.dto.TransferResponse;
import com.olukotun.payments.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/status")
    public Map<String, String> getStatus() {
        return Map.of(
                "service","payment-transfer-api",
                "status","running"
        );
    }
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(transferService.createTransfer(idempotencyKey, request));
    }

    @GetMapping("{id}")
    public ResponseEntity<TransferResponse> getTransferDetails(@PathVariable UUID id){
        return ResponseEntity.status(HttpStatus.OK).body(transferService.getTransfer(id));
    }
}
