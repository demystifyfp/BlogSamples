CREATE TYPE event_type AS ENUM ('domain', 'oms', 'system');

ALTER TABLE event ALTER COLUMN channel_id DROP NOT NULL;
ALTER TABLE event ALTER COLUMN channel_name DROP NOT NULL;
ALTER TABLE event ADD COLUMN type event_type NOT NULL DEFAULT 'domain';
ALTER TABLE event ADD COLUMN payload JSONB NOT NULL DEFAULT '{}';