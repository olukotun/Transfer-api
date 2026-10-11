CREATE TABLE transfer_outbox (
                                 id UUID PRIMARY KEY,
                                 transfer_id UUID NOT NULL REFERENCES transfers(id),
                                 event_type VARCHAR(50) NOT NULL,
                                 created_at TIMESTAMP WITH TIME ZONE NOT NULL
                                     DEFAULT CURRENT_TIMESTAMP,
                                 published_at TIMESTAMP WITH TIME ZONE,

                                 CONSTRAINT uk_transfer_outbox_event
                                     UNIQUE (transfer_id, event_type)
);

CREATE INDEX idx_transfer_outbox_unpublished
    ON transfer_outbox (created_at, id)
    WHERE published_at IS NULL;