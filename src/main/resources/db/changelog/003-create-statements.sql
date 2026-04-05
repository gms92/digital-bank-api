CREATE TABLE statements (
    id          UUID           NOT NULL,
    account_id  UUID           NOT NULL,
    transfer_id UUID           NOT NULL,
    type        VARCHAR(10)    NOT NULL,
    amount      DECIMAL(19, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT pk_statements PRIMARY KEY (id),
    CONSTRAINT fk_statements_account  FOREIGN KEY (account_id)  REFERENCES accounts (id),
    CONSTRAINT fk_statements_transfer FOREIGN KEY (transfer_id) REFERENCES transfers (id)
);
