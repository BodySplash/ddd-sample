package lib.eventsourcing

import com.google.common.flogger.FluentLogger
import kotlinx.serialization.KSerializer
import lib.ddd.domain.AsyncEventHandler
import lib.ddd.domain.Event
import lib.ddd.domain.PlainEvent
import lib.ddd.domain.RawEvent


class EventStreamRunner<TEvent>(
    private val poller: EventPoller,
    private val asyncEventHandler: AsyncEventHandler<TEvent>,
    private val serializer: KSerializer<TEvent>
) : EventStreamConsumer {


    suspend fun run() {
        poller.fromLastPositionApply(asyncEventHandler.name, this)
    }

    suspend fun init() {
        poller.createSubscription(asyncEventHandler.name)
    }

    override suspend fun onStart(lastPosition: Long) {
        LOGGER.atFine().log("Starting projection %s from %s", asyncEventHandler.name, lastPosition)
        asyncEventHandler.onStart()
    }

    override suspend fun onEvent(event: RawEvent) {
        LOGGER.atFine().log("Processing event %s in %s", event.id.eventId, asyncEventHandler.name)
        asyncEventHandler.onEvent(event.toConcrete())
    }

    override suspend fun onEnd(lastPosition: Long) {
        LOGGER.atFine().log("Projection %s completed at %s", asyncEventHandler.name, lastPosition)
        asyncEventHandler.onEnd()
    }

    val name by asyncEventHandler::name

    private fun RawEvent.toConcrete(): Event<TEvent> {
        return PlainEvent(this.id, this.timestamp, eventStoreJson.decodeFromJsonElement(serializer, this.payload))
    }

    companion object {
        private val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()
    }
}
