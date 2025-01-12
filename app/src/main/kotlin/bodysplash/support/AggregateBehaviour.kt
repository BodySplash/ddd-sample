package bodysplash.support

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf

fun interface ReplyConsumer<in R> {
    fun accept(reply: R)
}

interface AggregateRef<TCommand> {
    suspend fun handle(command: TCommand)
}

sealed interface AggregateEffect<out TEvent> {
    data class Persist<out TEvent>(val events: List<TEvent>) : AggregateEffect<TEvent> {
        constructor(event: TEvent) : this(listOf(event))
    }

    data class Reply<TReply, out TEvent>(val reply: TReply, val consumer: ReplyConsumer<TReply>) :
        AggregateEffect<TEvent>

    data class Both<TEvent>(val effects: NonEmptyList<AggregateEffect<TEvent>>) : AggregateEffect<TEvent>
}

fun <TReply, TEvent> AggregateEffect.Persist<TEvent>.andReply(
    reply: TReply,
    consumer: ReplyConsumer<TReply>
): AggregateEffect<TEvent> = AggregateEffect.Both(nonEmptyListOf(this, AggregateEffect.Reply(reply, consumer)))

interface AggregateBehaviour<ID, Command, State, Event> {

    fun initialState(): State

    fun decide(id: ID, command: Command, state: State): AggregateEffect<Event>

    fun evolve(state: State, event: Event): State

}
