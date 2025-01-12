CREATE TABLE event_store
(
    id             BIGSERIAL PRIMARY KEY,
    transaction_id XID8         NOT NULL       DEFAULT '0'::xid8,
    aggregate_id   VARCHAR(256) NOT NULL,
    aggregate_type VARCHAR(250) NOT NULL,
    event_type     VARCHAR(250) NOT NULL,
    timestamp      timestamp(3) with time zone default CURRENT_TIMESTAMP,
    payload        JSONB
);

CREATE
    INDEX event_store_aggregate_index
    ON event_store (aggregate_id, aggregate_type);

CREATE
    INDEX event_store_timestamp_index
    ON event_store (timestamp);

create index event_store_transaction_id_idx on event_store (transaction_id, id);