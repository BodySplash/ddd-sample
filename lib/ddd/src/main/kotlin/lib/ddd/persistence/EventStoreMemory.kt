package lib.ddd.persistence

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.serialization.json.JsonElement
import lib.ddd.domain.EventHeader
import lib.ddd.domain.EventId
import lib.ddd.domain.PlainEvent
import lib.ddd.domain.RawEvent
import java.time.Instant

class EventStoreMemory : EventStore {

    override suspend fun allOf(
        id: String,
        collection: String
    ): Flow<RawEvent> = events.computeIfAbsent(collection) { mutableListOf() }
        .filter { e -> e.id.targetId == id }
        .asFlow()

    override suspend fun allOfAfter(
        id: String,
        fromEvent: Long,
        collection: String
    ): Flow<RawEvent> =
        events.computeIfAbsent(collection) { mutableListOf() }.filter { e ->
            e.id.targetId == id &&
                    e.id.eventId > fromEvent
        }.asFlow()


    override suspend fun save(
        id: String,
        collection: String,
        events: Collection<JsonElement>
    ): Collection<EventHeader> {
        val savedEvents = events.map { PlainEvent(EventId(counter++, id), Instant.now(), it) }
        this.events.computeIfAbsent(collection) { mutableListOf() }.addAll(savedEvents)
        return savedEvents.map { it.header }
    }

    private val events = mutableMapOf<String, MutableList<RawEvent>>()
    private var counter: Long = 0
}
