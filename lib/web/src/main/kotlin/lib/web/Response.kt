package lib.web

import arrow.core.raise.Raise
import arrow.core.raise.fold
import com.google.common.flogger.FluentLogger
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import lib.common.BusinessError
import kotlin.experimental.ExperimentalTypeInference


private val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()

@OptIn(ExperimentalTypeInference::class)
suspend inline fun <reified A : Any> RoutingContext.syncResponse(
    status: HttpStatusCode = HttpStatusCode.OK,
    @BuilderInference block: Raise<Throwable>.() -> A
): Unit =
    fold(
        { block.invoke(this) },
        { err -> respondWhenError(err) },
        { r -> respondWhenError(r) },
        { r -> call.respond(status, r) })


suspend fun RoutingContext.respondWhenError(
    error: Throwable,
) = when (error) {

    is BusinessError -> {
        val code = if (error.code == "ENTITY_NOT_FOUND") {
            HttpStatusCode.NotFound
        } else {
            HttpStatusCode.BadRequest
        }
        call.respond(code, buildJsonObject {
            put("code", error.code)
            put("message", error.message.orEmpty())
        })
    }

    is ContentTransformationException -> {
        call.respond(HttpStatusCode.BadRequest, buildJsonObject {
            put("code", "BAD_PAYLOAD")
            put("message", error.message.orEmpty())
        })
    }

    else -> {
        LOGGER.atSevere().withCause(error).log("Error processing request")
        call.respond(HttpStatusCode.InternalServerError)
    }
}