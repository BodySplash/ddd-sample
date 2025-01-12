package lib.eventsourcing

import kotlinx.serialization.json.Json


internal val eventStoreJson = Json {
    ignoreUnknownKeys = true
}