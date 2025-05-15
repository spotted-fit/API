package fit.spotted.api.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class OkResponse(val result: String = "ok", val response: JsonElement)