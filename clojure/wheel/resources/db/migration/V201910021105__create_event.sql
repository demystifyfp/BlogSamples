CREATE TYPE event_level AS ENUM (
  'info', 'debug',
  'error', 'warn',
  'fatal');

CREATE TYPE channel_name AS ENUM (
  'tata-cliq', 'amazon', 'flipkart');

CREATE TABLE event (
  id UUID PRIMARY KEY,
  parent_id UUID REFERENCES event(id),
  level event_level NOT NULL,
  name TEXT NOT NULL,
  channel_id TEXT NOT NULL,
  channel_name channel_name NOT NULL,
  timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);