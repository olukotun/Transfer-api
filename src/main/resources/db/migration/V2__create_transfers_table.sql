CREATE TABLE transfers (
                           id UUID PRIMARY KEY,
                           source_account VARCHAR(50) NOT NULL,
                           destination_account VARCHAR(50) NOT NULL,
                           amount NUMERIC(14, 2) NOT NULL,
                           currency VARCHAR(3) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                           CONSTRAINT chk_transfer_amount CHECK (amount > 0),
                           CONSTRAINT chk_transfer_currency CHECK (currency = 'USD'),
                           CONSTRAINT chk_different_accounts
                               CHECK (source_account <> destination_account)
);