CREATE TABLE subscription (
    client_id    VARCHAR(64) PRIMARY KEY,
    api_key      VARCHAR(128) NOT NULL UNIQUE,
    webhook_url  VARCHAR(2048) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE notification_event (
    event_id     VARCHAR(64) PRIMARY KEY,
    client_id    VARCHAR(64) NOT NULL REFERENCES subscription (client_id),
    event_type   VARCHAR(128) NOT NULL,
    content      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_notification_event_client_created
    ON notification_event (client_id, created_at DESC);

CREATE TABLE delivery_attempt (
    event_id           VARCHAR(64) PRIMARY KEY REFERENCES notification_event (event_id),
    status             VARCHAR(20) NOT NULL,
    retry_count        INT NOT NULL DEFAULT 0,
    max_retries        INT NOT NULL DEFAULT 5,
    next_retry_at      TIMESTAMPTZ,
    last_attempted_at  TIMESTAMPTZ,
    last_http_status   INT,
    last_error         TEXT,
    completed_at       TIMESTAMPTZ
);

CREATE INDEX idx_delivery_attempt_status_next_retry
    ON delivery_attempt (status, next_retry_at);
