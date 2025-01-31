/*
 * This file is generated by jOOQ.
 */
package lib.eventsourcing.schema;


import java.util.Arrays;
import java.util.List;

import lib.eventsourcing.schema.tables.AggregateMetadata;
import lib.eventsourcing.schema.tables.EventOutbox;
import lib.eventsourcing.schema.tables.EventStore;

import org.jooq.Catalog;
import org.jooq.Table;
import org.jooq.impl.SchemaImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class EventStoreSchema extends SchemaImpl {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>event_store_schema</code>
     */
    public static final EventStoreSchema EVENT_STORE_SCHEMA = new EventStoreSchema();

    /**
     * The table <code>event_store_schema.aggregate_metadata</code>.
     */
    public final AggregateMetadata AGGREGATE_METADATA = AggregateMetadata.AGGREGATE_METADATA;

    /**
     * The table <code>event_store_schema.event_outbox</code>.
     */
    public final EventOutbox EVENT_OUTBOX = EventOutbox.EVENT_OUTBOX;

    /**
     * The table <code>event_store_schema.event_store</code>.
     */
    public final EventStore EVENT_STORE = EventStore.EVENT_STORE;

    /**
     * No further instances allowed
     */
    private EventStoreSchema() {
        super("event_store_schema", null);
    }


    @Override
    public Catalog getCatalog() {
        return DefaultCatalog.DEFAULT_CATALOG;
    }

    @Override
    public final List<Table<?>> getTables() {
        return Arrays.asList(
            AggregateMetadata.AGGREGATE_METADATA,
            EventOutbox.EVENT_OUTBOX,
            EventStore.EVENT_STORE
        );
    }
}
