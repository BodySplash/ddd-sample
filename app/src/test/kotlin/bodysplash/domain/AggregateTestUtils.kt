package bodysplash.domain

import bodysplash.support.AggregateBehaviour
import bodysplash.support.AggregateEffect

data class AggregateResult<State, Event>(val newState: State, val events: List<Event> = emptyList())


fun <ID, Command, State, Event> AggregateBehaviour<ID, Command, State, Event>.testRun(
    id: ID,
    command: Command,
    state: State = initialState()
): AggregateResult<State, Event> {

    fun processResult(
        acc: AggregateResult<State, Event>,
        current: AggregateEffect<Event>
    ): AggregateResult<State, Event> {
        return when (current) {
            is AggregateEffect.Both<Event> ->
                listOf(current.persist, current.reply).fold(acc, ::processResult)

            is AggregateEffect.Persist<Event> ->
                acc.copy(
                    events = acc.events + current.events,
                    newState = current.events.fold(acc.newState) { acc, event -> evolve(acc, event) })


            is AggregateEffect.Reply<*, *> -> {
                current.sendReply()
                acc
            }
        }
    }

    return processResult(AggregateResult(state), decide(id, command, state))
}

private fun <Reply> AggregateEffect.Reply<Reply, *>.sendReply() = consumer.accept(reply)