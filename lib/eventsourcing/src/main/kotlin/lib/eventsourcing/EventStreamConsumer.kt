package lib.eventsourcing

import lib.ddd.domain.RawEvent

interface EventStreamConsumer {
    suspend fun onStart(lastPosition: Long) {}
    suspend fun onEnd(lastPosition: Long) {}
    suspend fun onEvent(event: RawEvent)
}
