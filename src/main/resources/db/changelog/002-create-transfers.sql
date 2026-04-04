CREATE TABLE transfers (
    id                UUID           NOT NULL,
    source_account_id UUID           NOT NULL,
    target_account_id UUID           NOT NULL,
    amount            DECIMAL(19, 2) NOT NULL,
    created_at        TIMESTAMP      NOT NULL,
    CONSTRAINT pk_transfers PRIMARY KEY (id),
    CONSTRAINT fk_transfers_source FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transfers_target FOREIGN KEY (target_account_id) REFERENCES accounts (id)
);
