package bodysplash.support

import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonElement
import lib.ddd.persistence.EventStore

interface AggregatePersistence<ID, Event> {
    fun serializeId(id: ID): String
    fun serialize(event: Event): JsonElement
    fun deserialize(json: JsonElement): Event
}

class Repository<ID, Command, State, Event>(
    private val behaviour: AggregateBehaviour<ID, Command, State, Event>,
    private val persistence: AggregatePersistence<ID, Event>,
    private val store: EventStore,
) {

    suspend fun get(id: ID): AggregateRef<Command> {
        val state = store.allOf(id.toString(), behaviour.javaClass.simpleName)
            .map { e -> persistence.deserialize(e.payload) }
            .fold(behaviour.initialState()) { acc, event -> behaviour.evolve(acc, event) }
        return AggregateRefImpl(id, state)
    }

    private inner class AggregateRefImpl(
        private var id: ID,
        private var state: State,
    ) :
        AggregateRef<Command> {

        override suspend fun handle(command: Command) {
            process(behaviour.decide(id, command, state))
        }

        private suspend fun process(effect: AggregateEffect<Event>) {
            when (effect) {
                is AggregateEffect.Both<Event> -> effect.effects.forEach { e -> process(e) }
                is AggregateEffect.Persist<Event> -> {
                    store.save(
                        persistence.serializeId(id),
                        behaviour.javaClass.simpleName,
                        effect.events.map { e -> persistence.serialize(e) }
                    )
                    state = effect.events.fold(state) { acc, event -> behaviour.evolve(acc, event) }
                }

                is AggregateEffect.Reply<*, Event> -> effect.exec()
            }
        }

        private fun <TReply> AggregateEffect.Reply<TReply, *>.exec() = consumer.accept(reply)
    }
}

