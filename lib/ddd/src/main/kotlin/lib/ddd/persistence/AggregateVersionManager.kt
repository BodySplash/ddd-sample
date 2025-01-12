package lib.ddd.persistence

import arrow.core.Either
import arrow.core.Option
import kotlinx.serialization.json.JsonElement

enum class Concurrency {
    OPTIMISTIC,
    PESSIMISTIC
}

interface AggregateVersionManager {
    suspend fun load(
        id: String,
        aggregateType: String,
        concurrency: Concurrency = Concurrency.OPTIMISTIC
    ): Option<AggregateMetadata>

    suspend fun init(id: String, aggregateType: String, aggregateTypeVersion: Int): Either<Throwable, AggregateMetadata>

    suspend fun incrementVersion(
        id: String,
        aggregateType: String,
        currentVersion: Int,
        nextVersion: Int,
    ): Either<Throwable, AggregateMetadata>

    suspend fun incrementVersionWithSnapshot(
        id: String,
        aggregateType: String,
        nextTypeVersion: Int,
        currentVersion: Int,
        nextVersion: Int,
        offset: Long,
        snapshot: JsonElement,
    ): Either<Throwable, AggregateMetadata>
}
