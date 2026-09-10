CREATE TABLE atlas_command (
    id VARCHAR(30) PRIMARY KEY,

    actor_type TEXT NOT NULL,
    actor_id TEXT,

    origin_type TEXT NOT NULL,
    origin_id TEXT,

    device_id TEXT NOT NULL,
    action TEXT NOT NULL,
    parameters JSONB NOT NULL DEFAULT '{}'::jsonb,

    criticality TEXT NOT NULL,

    requested_at TIMESTAMPTZ NOT NULL,
    dispatched_at TIMESTAMPTZ,
    executing_since TIMESTAMPTZ,
    expected_completion_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    timeout_ms BIGINT NOT NULL,

    idempotency_value TEXT NOT NULL,
    idempotency_window_ms BIGINT NOT NULL,

    trace_id TEXT NOT NULL,
    cause_type TEXT,
    cause_id TEXT,
    depth INTEGER NOT NULL,

    result_outcome TEXT,
    result_error_code TEXT,
    result_error_message TEXT,
    result_adapter_message_id TEXT,
    result_at TIMESTAMPTZ,

    CONSTRAINT chk_atlas_command_depth
        CHECK (depth >= 0),

    CONSTRAINT chk_atlas_command_timeout
        CHECK (timeout_ms > 0),

    CONSTRAINT chk_atlas_command_idempotency_window
        CHECK (idempotency_window_ms > 0)
);


CREATE TABLE atlas_command_status_history (
    command_id VARCHAR(30) NOT NULL,
    sequence_no INTEGER NOT NULL,
    status TEXT NOT NULL,
    at TIMESTAMPTZ NOT NULL,
    detail TEXT,

    PRIMARY KEY (command_id, sequence_no),

    CONSTRAINT fk_command_status_command
        FOREIGN KEY (command_id)
        REFERENCES atlas_command(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_command_status_sequence
        CHECK (sequence_no >= 0)
);


CREATE INDEX idx_atlas_command_requested_at
    ON atlas_command(requested_at);

CREATE INDEX idx_atlas_command_device_id
    ON atlas_command(device_id);

CREATE INDEX idx_atlas_command_trace_id
    ON atlas_command(trace_id);

CREATE INDEX idx_atlas_command_idempotency_value
    ON atlas_command(idempotency_value);

CREATE INDEX idx_atlas_command_status_at
    ON atlas_command_status_history(at);
