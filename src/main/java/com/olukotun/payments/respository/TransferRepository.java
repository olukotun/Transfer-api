package com.olukotun.payments.respository;

import com.olukotun.payments.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);

    @Query(value = """
        SELECT *
        FROM transfers
        WHERE status = 'PENDING'
        ORDER BY created_at, id
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<Transfer> findNextPendingForUpdate();
}
