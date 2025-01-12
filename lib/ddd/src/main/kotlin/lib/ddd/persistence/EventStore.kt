package lib.ddd.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement
import lib.ddd.domain.EventHeader
import lib.ddd.domain.RawEvent


interface EventStore {
    suspend fun save(id: String, collection: String, events: Collection<JsonElement>): Collection<EventHeader>
    suspend fun allOf(id: String, collection: String): Flow<RawEvent>
    suspend fun allOfAfter(
        id: String,
        fromEvent: Long,
        collection: String,
    ): Flow<RawEvent>
}
