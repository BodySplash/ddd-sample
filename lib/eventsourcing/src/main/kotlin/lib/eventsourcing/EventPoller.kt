package lib.eventsourcing

import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import lib.database.jooq.await
import lib.database.jooq.fetchManyAsync
import lib.database.jooq.fetchOneAsync
import lib.database.jooq.flow
import lib.database.jooq.withTransaction
import lib.ddd.domain.EventId
import lib.ddd.domain.PlainEvent
import lib.ddd.domain.RawEvent
import lib.eventsourcing.schema.Tables.EVENT_OUTBOX
import lib.eventsourcing.schema.Tables.EVENT_STORE
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.currentDsl
import org.jooq.impl.currentDslOrDefault
import java.util.concurrent.TimeUnit

private val EO_LAST_TRANSACTION
    get() = DSL.field("last_transaction_id::text", String::class.java)

internal val ES_TRANSACTION_ID
    get() = DSL.field("transaction_id::text", String::class.java)

private const val initialDelay = 500L
private const val factor = 1.5
private const val maxDelay = 5000L

class EventPoller(private val dsl: DSLContext) {
    suspend fun createSubscription(groupName: String) = dsl.withTransaction { tx ->
        tx.insertInto(EVENT_OUTBOX)
            .columns(EVENT_OUTBOX.SUBSCRIPTION_GROUP, EVENT_OUTBOX.LAST_ID, EVENT_OUTBOX.LAST_TRANSACTION_ID)
            .values(groupName, -1L, DSL.field("?::xid8", "0"))
            .onConflictDoNothing()
            .await()
    }

    suspend fun waitForCheckPoint(checkpoint: Checkpoint) {
        val checkpointTransaction = checkpoint.lastTransaction
        var maxTransactionId = maxTransactionId()
        var currentDelay = initialDelay
        while (checkpointTransaction > maxTransactionId) {
            LOGGER.atInfo().atMostEvery(10, TimeUnit.SECONDS)
                .log("Waiting for checkpoint %s. At %s", checkpoint, maxTransactionId)
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            maxTransactionId = maxTransactionId()
        }
    }

    private suspend fun maxTransactionId(): ULong {
        val field = DSL.field(
            "pg_snapshot_xmin(pg_current_snapshot())::text",
            String::class.java
        )
        val txId = currentDsl().select(field).fetchOneAsync(field)
        return txId!!.toULong()
    }

    suspend fun fromLastPositionApply(groupName: String, consumer: EventStreamConsumer): Long =
        dsl.withTransaction {
            val position = readLastEntryAndGetLock(groupName) ?: return@withTransaction -1
            fetchFrom(position.lastTransaction, position.lastPosition).let { eventStream ->
                consumer.onStart(position.lastPosition)
                val lastCheckpoint = eventStream.fold(EMPTY_CHECKPOINT) { _, e ->
                    consumer.onEvent(e)
                    e.checkpoint
                }
                consumer.onEnd(lastCheckpoint.lastPosition)
                if (lastCheckpoint != EMPTY_CHECKPOINT) {
                    updateLastEntry(groupName, lastCheckpoint)
                }
                lastCheckpoint.lastPosition
            }
        }


    suspend fun readLastEntryAndGetLock(groupName: String?): GroupPosition? =
        transactionalContext().select(EVENT_OUTBOX.LAST_ID, EVENT_OUTBOX.MAX_ID, EO_LAST_TRANSACTION)
            .from(EVENT_OUTBOX)
            .where(EVENT_OUTBOX.SUBSCRIPTION_GROUP.eq(groupName))
            .forUpdate()
            .skipLocked()
            .fetchOneAsync { r ->
                GroupPosition(
                    EO_LAST_TRANSACTION[r]!!.toULong(),
                    EVENT_OUTBOX.LAST_ID[r]!!,
                    EVENT_OUTBOX.MAX_ID[r],
                )
            }

    suspend fun updateLastEntry(groupName: String, checkpoint: Checkpoint) =
        transactionalContext().update(EVENT_OUTBOX)
            .set(
                DSL.row(EVENT_OUTBOX.LAST_ID, EVENT_OUTBOX.LAST_TRANSACTION_ID),
                DSL.row(checkpoint.lastPosition, DSL.field("?::xid8", checkpoint.lastTransaction.toString()))
            )
            .where(EVENT_OUTBOX.SUBSCRIPTION_GROUP.eq(groupName))
            .await()

    internal suspend fun fetchFrom(lastTransaction: ULong, lastId: Long): Flow<EventWithCheckPoint> =
        currentDsl().select(
            EVENT_STORE.ID,
            EVENT_STORE.AGGREGATE_ID,
            EVENT_STORE.TIMESTAMP,
            EVENT_STORE.PAYLOAD,
            ES_TRANSACTION_ID,
        ).from(EVENT_STORE)
            .where("(transaction_id, id) > (?::xid8, ?)", lastTransaction.toString(), lastId)
            .and("transaction_id < pg_snapshot_xmin(pg_current_snapshot())")
            .orderBy(EVENT_STORE.TRANSACTION_ID.asc(), EVENT_STORE.ID.asc())
            .flow()
            .map { record: Record -> toEvent(record) }


    suspend fun catchupGroups(): List<String> =
        currentDsl().select(EVENT_OUTBOX.SUBSCRIPTION_GROUP)
            .from(EVENT_OUTBOX)
            .where(EVENT_OUTBOX.MAX_ID.isNotNull())
            .and(EVENT_OUTBOX.MAX_ID.greaterThan(EVENT_OUTBOX.LAST_ID))
            .fetchManyAsync(EVENT_OUTBOX.SUBSCRIPTION_GROUP)

    private fun toEvent(record: Record): EventWithCheckPoint {
        val eventId = EVENT_STORE.ID[record]
        val targetId = EVENT_STORE.AGGREGATE_ID[record]
        val timestamp = EVENT_STORE.TIMESTAMP[record]
        val payload = EVENT_STORE.PAYLOAD[record]
        val transactionId = ES_TRANSACTION_ID[record]!!

        val raw = PlainEvent(
            EventId(eventId!!, targetId!!),
            timestamp!!.toInstant(),
            Json.parseToJsonElement(payload!!.data())
        )
        return EventWithCheckPoint(
            checkpoint = Checkpoint(
                lastTransaction = transactionId.toULong(),
                lastPosition = eventId,
            ),
            raw = raw,
        )
    }

    private suspend fun transactionalContext() = currentCoroutineContext().currentDsl()

    private suspend fun currentDsl(): DSLContext = currentCoroutineContext().currentDslOrDefault(dsl)

    data class GroupPosition(val lastTransaction: ULong, val lastPosition: Long, val maxPosition: Long?)

    companion object {
        private val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()
    }
}

data class Checkpoint(val lastTransaction: ULong, val lastPosition: Long) {
    companion object {
        fun toNotification(lastId: Long, lastTransaction: String) = "$lastTransaction:$lastId"

        fun fromNotification(notification: String): Checkpoint {
            val (lastTransaction, lastId) = notification.split(":")
            return Checkpoint(lastTransaction.toULong(), lastId.toLong())
        }
    }
}

data class EventWithCheckPoint(val checkpoint: Checkpoint, val raw: RawEvent) : RawEvent by raw

private val EMPTY_CHECKPOINT = Checkpoint(0u, -1)