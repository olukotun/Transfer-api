package com.olukotun.payments.respository;

import com.olukotun.payments.entity.TransferOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferOutboxRepository extends JpaRepository<TransferOutbox, UUID> {

    long countByTransferId(UUID transferId);

    void deleteByTransferId(UUID transferId);
}