-- Demo subscriptions for the 3 clients present in notification_events.json.
--
-- TODO: webhook_url below is a placeholder. Once the real destination URL is provided on
-- presentation day, update these rows (or set WEBHOOK_DEFAULT_URL and re-seed) — see
-- README.md "How to point it at the real endpoint". No code change is required either way.
INSERT INTO subscription (client_id, api_key, webhook_url, active) VALUES
    ('CLIENT001', 'demo-api-key-client001', 'https://example.com/webhooks/client001', true),
    ('CLIENT002', 'demo-api-key-client002', 'https://example.com/webhooks/client002', true),
    ('CLIENT003', 'demo-api-key-client003', 'https://example.com/webhooks/client003', true);
