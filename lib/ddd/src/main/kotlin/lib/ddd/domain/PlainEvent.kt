package lib.ddd.domain

import kotlinx.serialization.json.JsonElement
import java.time.Instant


@JvmRecord
data class EventId(
    val eventId: Long,
    val targetId: String,
)

data class EventHeader(
    val id: EventId,
    val timestamp: Instant,
)

interface Event<out T> {
    val id: EventId
    val timestamp: Instant
    val payload: T
}

data class PlainEvent<out T>(val header: EventHeader, override val payload: T) :
    Event<T> {
    constructor(id: EventId, timestamp: Instant, payload: T) : this(EventHeader(id, timestamp), payload)

    override val id: EventId by header::id

    override val timestamp: Instant by header::timestamp
}


typealias RawEvent = Event<JsonElement>
