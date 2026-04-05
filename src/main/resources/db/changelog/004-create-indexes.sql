CREATE INDEX idx_transfers_source_account_id ON transfers (source_account_id);
CREATE INDEX idx_transfers_target_account_id ON transfers (target_account_id);
CREATE INDEX idx_statements_account_id ON statements (account_id);
CREATE INDEX idx_statements_transfer_id ON statements (transfer_id);
