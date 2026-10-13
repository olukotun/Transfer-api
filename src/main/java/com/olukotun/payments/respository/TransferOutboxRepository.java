package com.olukotun.payments.respository;

import com.olukotun.payments.entity.TransferOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TransferOutboxRepository extends JpaRepository<TransferOutbox, UUID> {

    long countByTransferId(UUID transferId);

    void deleteByTransferId(UUID transferId);

    @Query(value = """
        SELECT *
        FROM transfer_outbox
        WHERE published_at IS NULL
        ORDER BY created_at, id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<TransferOutbox> findNextUnpublishedForUpdate();
}