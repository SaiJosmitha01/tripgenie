CREATE TABLE IF NOT EXISTS trip_audit_logs (
    id UUID PRIMARY KEY,
    user_id UUID,
    action_type VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL,
    correlation_id VARCHAR(120),
    metadata TEXT,
    CONSTRAINT ck_trip_audit_logs_status CHECK (status IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX IF NOT EXISTS idx_trip_audit_logs_action_occurred
    ON trip_audit_logs(action_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_trip_audit_logs_user_occurred
    ON trip_audit_logs(user_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_trip_audit_logs_entity_occurred
    ON trip_audit_logs(entity_type, entity_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_trip_audit_logs_status_occurred
    ON trip_audit_logs(status, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_trip_audit_logs_correlation_id
    ON trip_audit_logs(correlation_id);
