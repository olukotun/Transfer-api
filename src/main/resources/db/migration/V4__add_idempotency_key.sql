ALTER TABLE transfers
    ADD COLUMN idempotency_key VARCHAR(100);

UPDATE transfers
SET idempotency_key = 'legacy-' || id::text
WHERE idempotency_key IS NULL;

ALTER TABLE transfers
    ALTER COLUMN idempotency_key SET NOT NULL;

ALTER TABLE transfers
    ADD CONSTRAINT uk_transfers_idempotency_key
        UNIQUE (idempotency_key);