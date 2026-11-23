DROP TABLE import_job_rows;
DROP TABLE import_jobs;

CREATE UNIQUE INDEX idx_csv_import_applied ON audit_events(entity_id) WHERE event_type='CSV_IMPORT';

ALTER TABLE refresh_sessions ADD COLUMN revoked_at TIMESTAMP WITH TIME ZONE;
UPDATE refresh_sessions s SET revoked_at=CURRENT_TIMESTAMP
FROM users u WHERE u.id=s.user_id AND u.is_blocked;
