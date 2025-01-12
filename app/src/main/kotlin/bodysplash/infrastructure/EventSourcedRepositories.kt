package bodysplash.infrastructure

import bodysplash.domain.*
import bodysplash.support.AggregatePersistence
import bodysplash.support.Repository
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import lib.ddd.persistence.EventStore

private val format = Json {
    ignoreUnknownKeys = true
}

class EventSourcedRepositories(eventStore: EventStore) : Repositories {


    override val games: Repository<GameId, GameCommand, GameState, GameEvent> = Repository(
        Mastermind, persistenceFor(GameEvent.serializer()) { id -> id.value.toString() }, eventStore
    )
}

private fun <T, TID> persistenceFor(
    serializer: KSerializer<T>,
    idSerializer: (TID) -> String
): AggregatePersistence<TID, T> = object : AggregatePersistence<TID, T> {
    override fun serializeId(id: TID): String = idSerializer(id)

    override fun serialize(event: T): JsonElement = format.encodeToJsonElement(serializer, event)

    override fun deserialize(json: JsonElement): T = format.decodeFromJsonElement(serializer, json)

}