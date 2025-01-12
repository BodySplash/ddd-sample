package lib.ddd.persistence

import arrow.core.Option
import kotlinx.serialization.json.JsonElement

@JvmRecord
data class Snapshot(val offset: Long, val payload: JsonElement)

@JvmRecord
data class AggregateMetadata(
    val id: String,
    val version: Int,
    val snapshot: Option<Snapshot>
)
