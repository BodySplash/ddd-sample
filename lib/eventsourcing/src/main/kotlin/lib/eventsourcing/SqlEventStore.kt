package lib.eventsourcing

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import lib.database.jooq.fetchManyAsync
import lib.database.jooq.flow
import lib.database.jooq.notify
import lib.ddd.domain.EventHeader
import lib.ddd.domain.EventId
import lib.ddd.domain.PlainEvent
import lib.ddd.domain.RawEvent
import lib.ddd.persistence.EventStore
import lib.eventsourcing.schema.Tables.EVENT_STORE
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.currentDsl
import org.jooq.impl.currentDslOrDefault
import java.time.OffsetDateTime

internal val ES_TRANSACTION_ID
    get() = DSL.field("transaction_id::text", String::class.java)

data class Checkpoint(val lastTransaction: ULong, val lastPosition: Long) {
    companion object {
        fun toNotification(lastId: Long, lastTransaction: String) = "$lastTransaction:$lastId"
    }
}

class SqlEventStore(
    dsl: DSLContext,
    schema: String,
) : EventStore {


    private val notificationChannel = "${schema}_$NOTIFICATION_CHANNEL"

    override suspend fun save(
        id: String,
        collection: String,
        events: Collection<JsonElement>
    ): Collection<EventHeader> {
        if (events.isEmpty()) {
            return listOf()
        }
        val now = OffsetDateTime.now()
        val persistedIds =
            events.fold(
                transactionalContext().insertInto(
                    EVENT_STORE,
                    EVENT_STORE.AGGREGATE_ID,
                    EVENT_STORE.AGGREGATE_TYPE,
                    EVENT_STORE.EVENT_TYPE,
                    EVENT_STORE.TIMESTAMP,
                    EVENT_STORE.PAYLOAD,
                    EVENT_STORE.TRANSACTION_ID,
                ),
            ) { statement, event ->
                if (event.jsonObject["type"] == null) {
                    throw IllegalArgumentException("Event type is missing")
                }
                statement.values(
                    id,
                    collection,
                    event.jsonObject["type"]!!.jsonPrimitive.content,
                    now,
                    JSONB.valueOf(event.toString()),
                    DSL.field("pg_current_xact_id()")
                )
            }.returningResult(EVENT_STORE.ID, ES_TRANSACTION_ID)
                .fetchManyAsync()

        val persistedEvents = persistedIds.map { (eventId) -> EventHeader(EventId(eventId, id), now.toInstant()) }

        val (lastId, txId) = persistedIds.last()
        transactionalContext().notify(notificationChannel, Checkpoint.toNotification(lastId, txId))
        return persistedEvents
    }


    override suspend fun allOf(id: String, collection: String): Flow<RawEvent> = currentDsl().select()
        .from(EVENT_STORE)
        .where(EVENT_STORE.AGGREGATE_TYPE.eq(collection))
        .and(EVENT_STORE.AGGREGATE_ID.eq(id))
        .orderBy(EVENT_STORE.ID)
        .flow()
        .map { toEvent(it) }

    override suspend fun allOfAfter(
        id: String,
        fromEvent: Long,
        collection: String
    ): Flow<RawEvent> = currentDsl().select()
        .from(EVENT_STORE)
        .where(EVENT_STORE.AGGREGATE_TYPE.eq(collection))
        .and(EVENT_STORE.ID.greaterThan(fromEvent))
        .and(EVENT_STORE.AGGREGATE_ID.eq(id))
        .orderBy(EVENT_STORE.ID)
        .flow()
        .map { toEvent(it) }


    private fun toEvent(record: Record): RawEvent {
        val eventMetadata = EventId(
            record.getValue(EVENT_STORE.ID),
            record.getValue(EVENT_STORE.AGGREGATE_ID),
        )
        val payload = eventStoreJson.parseToJsonElement(EVENT_STORE.PAYLOAD[record]!!.data())
        return PlainEvent(eventMetadata, record.getValue(EVENT_STORE.TIMESTAMP).toInstant(), payload)
    }

    private suspend fun transactionalContext() = currentCoroutineContext().currentDsl()

    private val currentDsl = suspend { currentCoroutineContext().currentDslOrDefault(dsl) }

    companion object {
        const val NOTIFICATION_CHANNEL = "events"
    }

}
