package lib.eventsourcing

import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.KSerializer
import lib.ddd.domain.AsyncEventHandler
import lib.runtime.Agent

class EventStreamWorker<TEvent>(
    private val poller: EventPoller,
    private val listener: NotificationListener,
    asyncEventHandlers: Set<AsyncEventHandler<TEvent>>,
    serializer: KSerializer<TEvent>,
) : Agent {

    override val name = "EventStreamWorker"

    override suspend fun onStart() {
        LOGGER.atInfo().log("Starting EventStreamWorker")
        channel = listener.registerListener(SqlEventStore.Companion.NOTIFICATION_CHANNEL, ::runAll) { v ->
            v.map(Checkpoint::fromNotification)
        }
        eventStreamRunners.forEach {
            it.init()
        }
        LOGGER.atInfo().log("${eventStreamRunners.size} projections started")
    }

    override suspend fun doWork() {
        val value = channel.receive()
        value.maxByOrNull { it.lastTransaction }?.let {
            poller.waitForCheckPoint(it)
        }
        runAll()
    }

    private suspend fun runAll() = supervisorScope {
        eventStreamRunners.forEach {
            launch(exceptionHandler) {
                it.run()
            }
        }
    }


    private var eventStreamRunners: List<EventStreamRunner<*>> =
        asyncEventHandlers.map { EventStreamRunner(poller, it, serializer) }


    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        LOGGER.atSevere().withCause(exception).log("Failed to run projection")
    }

    private lateinit var channel: ReceiveChannel<List<Checkpoint>>

    companion object {
        private val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()
    }

}
