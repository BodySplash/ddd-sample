package bodysplash

import bodysplash.application.ApplicationPort
import bodysplash.application.askGame
import bodysplash.domain.Color
import bodysplash.domain.GameCommand
import bodysplash.domain.GameId
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import lib.web.syncResponse
import org.koin.ktor.ext.inject
import java.util.*

@Serializable
data class TurnRequest(val colors: List<Color>)

fun Application.configureRouting() {

    val applicationPort by inject<ApplicationPort>()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/games") {
            syncResponse {
                val id = applicationPort.askGame(GameId(UUID.randomUUID())) { r ->
                    GameCommand.Create(
                        (1..5).map { Color.entries.random() }, 3, r
                    )
                }.bind()
                mapOf("id" to id.value.toString())
            }
        }
        post("/games/{id}/guesses") {
            syncResponse {
                val payload = call.receive<TurnRequest>()
                applicationPort.askGame(GameId(UUID.fromString(call.parameters["id"]))) { r ->
                    GameCommand.Guess(payload.colors, r)
                }.bind()
            }
        }
    }
}
