package lib.eventsourcing

import lib.database.SqlBatchManager
import lib.ddd.domain.Event
import lib.ddd.domain.SyncEventHandler


interface SqlProjectionEventHandler<TEvent> : SyncEventHandler<TEvent> {

    override suspend fun onEvents(events: List<Event<TEvent>>) {
        events.fold(SqlBatchManager()) { acc, event ->
            onEvent(acc, event)
            acc
        }.flush()
    }

    suspend fun onEvent(batch: SqlBatchManager, event: Event<TEvent>)
}
