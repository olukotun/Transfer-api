package com.olukotun.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "transfers")
@Getter
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "source_account", nullable = false, length = 50)
    private String sourceAccount;

    @Column(name = "destination_account", nullable = false, length = 50)
    private String destinationAccount;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Transfer() {
        // Required by JPA.
    }

    public Transfer(
            String idempotencyKey,
            String sourceAccount,
            String destinationAccount,
            BigDecimal amount,
            String currency) {

        this.idempotencyKey = idempotencyKey;
        this.id = UUID.randomUUID();
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.status = "PENDING";
        this.createdAt = Instant.now();

    }

}
