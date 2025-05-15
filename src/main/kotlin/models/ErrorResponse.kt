package fit.spotted.api.models

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val result: String = "error", val message: String)