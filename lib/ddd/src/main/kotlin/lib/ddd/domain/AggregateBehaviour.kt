package lib.ddd.domain

fun interface ReplyConsumer<in R> {
    fun accept(reply: R)
}

interface AggregateRef<TCommand> {
    suspend fun handle(command: TCommand)
}

sealed interface AggregateEffect<out TEvent> {
    data class Persist<out TEvent>(val events: List<TEvent>) : AggregateEffect<TEvent>

    data class Reply<TReply, out TEvent>(val reply: TReply, val consumer: ReplyConsumer<TReply>) :
        AggregateEffect<TEvent>

    data class Both<TEvent>(
        val reply: Reply<*, TEvent>,
        val persist: Persist<TEvent>
    ) : AggregateEffect<TEvent>
}

fun <TEvent, Reply> AggregateEffect.Persist<TEvent>.andReply(reply: Reply, consumer: ReplyConsumer<Reply>) =
    AggregateEffect.Both(AggregateEffect.Reply(reply, consumer), this)


interface AggregateBehaviour<ID, Command, State, Event> {

    fun initialState(): State

    fun decide(id: ID, command: Command, state: State): AggregateEffect<Event>

    fun evolve(state: State, event: Event): State

}
