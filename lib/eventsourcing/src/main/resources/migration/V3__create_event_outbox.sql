create table event_outbox
(
    subscription_group  varchar(256) primary key,
    last_id             bigint,
    last_transaction_id XID8 NOT NULL DEFAULT '0'::xid8,
    max_id              bigint
);

