package com.olukotun.payments.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        String status,
        BigDecimal amount,
        String currency
) {
}
