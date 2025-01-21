package bodysplash.infrastructure

import bodysplash.domain.GameEvent
import bodysplash.domain.Mastermind
import bodysplash.domain.Repositories
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import lib.common.TransactionProvider
import lib.ddd.domain.AggregateSerializer
import lib.ddd.domain.SimpleRepository
import lib.ddd.persistence.EventStore

private val format = Json {
    ignoreUnknownKeys = true
}

class EventSourcedRepositories(eventStore: EventStore, transactionProvider: TransactionProvider) : Repositories {

    override val games = SimpleRepository(
        Mastermind,
        serializerFor(GameEvent.serializer()) { id -> id.value.toString() },
        eventStore,
        transactionProvider
    )
}

private fun <T, TID> serializerFor(
    serializer: KSerializer<T>,
    idSerializer: (TID) -> String
): AggregateSerializer<TID, T> = object : AggregateSerializer<TID, T> {

    override fun serializeId(id: TID): String = idSerializer(id)

    override fun serialize(event: T): JsonElement = format.encodeToJsonElement(serializer, event)

    override fun deserialize(json: JsonElement): T = format.decodeFromJsonElement(serializer, json)

}
