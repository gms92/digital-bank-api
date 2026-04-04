CREATE TABLE accounts (
    id          UUID           NOT NULL,
    name        VARCHAR(255)   NOT NULL,
    balance     DECIMAL(19, 2) NOT NULL,
    created_at  TIMESTAMP      NOT NULL,
    CONSTRAINT pk_accounts PRIMARY KEY (id)
);
