package lib.eventsourcing

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.raise.either
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.serialization.json.JsonElement
import lib.database.jooq.await
import lib.database.jooq.fetchOneAsync
import lib.ddd.persistence.*
import lib.eventsourcing.schema.Tables.AGGREGATE_METADATA
import org.jooq.DSLContext
import org.jooq.JSONB
import org.jooq.Record
import org.jooq.impl.currentDslOrDefault

class SqlAggregateVersionManager(private val context: DSLContext) : AggregateVersionManager {
    override suspend fun load(
        id: String,
        aggregateType: String,
        concurrency: Concurrency
    ): Option<AggregateMetadata> {
        val query = dslContext().select(
            AGGREGATE_METADATA.ID,
            AGGREGATE_METADATA.TYPE,
            AGGREGATE_METADATA.VERSION,
            AGGREGATE_METADATA.SNAPSHOT,
            AGGREGATE_METADATA.SNAPSHOT_OFFSET
        )
            .from(AGGREGATE_METADATA)
            .where(AGGREGATE_METADATA.ID.eq(id).and(AGGREGATE_METADATA.TYPE.eq(aggregateType)))
        val result =
            when (concurrency) {
                Concurrency.OPTIMISTIC -> query
                Concurrency.PESSIMISTIC -> query.forUpdate()
            }
                .fetchOneAsync { record: Record ->
                    val snapshot = if (AGGREGATE_METADATA.SNAPSHOT_OFFSET[record] != 0L) {
                        val payload = eventStoreJson.parseToJsonElement(AGGREGATE_METADATA.SNAPSHOT[record]?.data()!!)
                        Some(
                            Snapshot(
                                AGGREGATE_METADATA.SNAPSHOT_OFFSET[record]!!,
                                payload
                            )
                        )
                    } else {
                        None
                    }
                    AggregateMetadata(
                        id,
                        AGGREGATE_METADATA.VERSION[record]!!,
                        snapshot
                    )
                }

        return Option.fromNullable(result)
    }

    override suspend fun init(
        id: String,
        aggregateType: String,
        aggregateTypeVersion: Int,
    ): Either<Throwable, AggregateMetadata> =
        either {
            val count = dslContext().insertInto(AGGREGATE_METADATA)
                .columns(
                    AGGREGATE_METADATA.ID,
                    AGGREGATE_METADATA.TYPE,
                    AGGREGATE_METADATA.VERSION,
                    AGGREGATE_METADATA.SNAPSHOT_OFFSET
                )
                .values(id, aggregateType, 1, 0L)
                .onConflictDoNothing()
                .await()
            if (count == 0) {
                raise(ConcurrencyCheckError(id, aggregateType))
            }
            AggregateMetadata(id, 1, None)
        }


    override suspend fun incrementVersion(
        id: String,
        aggregateType: String,
        currentVersion: Int,
        nextVersion: Int
    ): Either<Throwable, AggregateMetadata> =
        either {
            val count = dslContext()
                .update(AGGREGATE_METADATA)
                .set(AGGREGATE_METADATA.VERSION, nextVersion)
                .where(AGGREGATE_METADATA.ID.eq(id))
                .and(AGGREGATE_METADATA.TYPE.eq(aggregateType))
                .and(AGGREGATE_METADATA.VERSION.eq(currentVersion))
                .await()
            if (count == 0) {
                raise(ConcurrencyCheckError(id, aggregateType))
            }
            AggregateMetadata(
                id,
                nextVersion,
                None
            )
        }

    override suspend fun incrementVersionWithSnapshot(
        id: String,
        aggregateType: String,
        nextTypeVersion: Int,
        currentVersion: Int,
        nextVersion: Int,
        offset: Long,
        snapshot: JsonElement
    ): Either<Throwable, AggregateMetadata> =
        either {
            val count = dslContext()
                .update(AGGREGATE_METADATA)
                .set(AGGREGATE_METADATA.VERSION, nextVersion)
                .set(AGGREGATE_METADATA.SNAPSHOT_OFFSET, offset)
                .set(AGGREGATE_METADATA.SNAPSHOT, JSONB.valueOf(snapshot.toString()))
                .where(AGGREGATE_METADATA.ID.eq(id))
                .and(AGGREGATE_METADATA.TYPE.eq(aggregateType))
                .and(AGGREGATE_METADATA.VERSION.eq(currentVersion))
                .await()

            if (count == 0) {
                raise(ConcurrencyCheckError(id, aggregateType))
            }

            AggregateMetadata(
                id,
                nextVersion,
                if (offset != 0L) Some(Snapshot(offset, snapshot)) else None
            )
        }

    private suspend fun dslContext() = currentCoroutineContext().currentDslOrDefault(context)
}
