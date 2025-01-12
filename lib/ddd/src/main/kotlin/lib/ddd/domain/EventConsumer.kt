package lib.ddd.domain

interface AsyncEventHandler<TEvent> {

    suspend fun onStart() {
    }

    suspend fun onEnd() {
    }

    suspend fun onEvent(event: Event<TEvent>)


    val name: String
}

interface SyncEventHandler<in TEvent> {

    suspend fun onEvents(events: List<Event<TEvent>>)


    val name:String
}

