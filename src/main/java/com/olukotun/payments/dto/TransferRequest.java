package com.olukotun.payments.dto;


import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank(message="Destination account is required")
        String sourceAccount,

        @NotBlank(message="Account is required")
        String destinationAccount,

        @NotNull(message = "Currency is required")
        @DecimalMin(value="0.01", message = "Amount must be at least 0.01")
        @Digits(integer = 12, fraction = 2, message ="Amount must have at most 12 integer digits and 2 decimal places" )
        BigDecimal amount,
        @Pattern(regexp = "USD", message = "Only USD is required")
        String currency

) {
}
