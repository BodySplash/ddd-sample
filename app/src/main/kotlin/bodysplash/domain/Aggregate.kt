package bodysplash.domain

sealed interface AggregateEffect<Event> {
    data class Persist<Event>(val events: List<Event>) : AggregateEffect<Event>

    data class Reply<Reply, Event>(val reply: Reply, val callback: (Reply) -> Unit) : AggregateEffect<Event>

    data class Both<Event>(val persist: Persist<Event>, val reply: Reply<*, Event>)
}

interface Aggregate<ID, Command, State, Event> {

    fun initialState(): State

    fun decide(id: ID, command: Command, state: State): Event

    fun evolve(state: State, event: Event): State

}