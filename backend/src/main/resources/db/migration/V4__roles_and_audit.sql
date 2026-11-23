CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    user_id BIGINT,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    description VARCHAR(500) NOT NULL,
    correlation_id VARCHAR(64)
);
CREATE INDEX idx_audit_created ON audit_events(created_at DESC);
