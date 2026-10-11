package com.olukotun.payments.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfer_outbox")
@Getter
public class TransferOutbox {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected TransferOutbox() {
        // Required by JPA.
    }

    public TransferOutbox(UUID transferId) {
        this.id = UUID.randomUUID();
        this.transferId = transferId;
        this.eventType = "TRANSFER_REQUESTED";
        this.createdAt = Instant.now();
    }
}