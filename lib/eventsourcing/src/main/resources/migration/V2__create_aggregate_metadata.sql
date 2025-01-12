create table aggregate_metadata
(
    id              varchar(256),
    type            varchar(256),
    version         int,
    snapshot_offset bigint,
    snapshot        jsonb,
    constraint aggregate_metadata_pk
        primary key (id, type)
);

