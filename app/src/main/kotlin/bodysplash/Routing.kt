package bodysplash

import bodysplash.application.ApplicationCommand
import bodysplash.application.ApplicationPort
import bodysplash.application.ask
import bodysplash.domain.Color
import bodysplash.domain.GameCommand
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import lib.web.syncResponse
import org.koin.ktor.ext.inject

fun Application.configureRouting() {

    val applicationPort by inject<ApplicationPort>()
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        post("/games") {
            syncResponse {
                val id = applicationPort.ask { r ->
                    ApplicationCommand.Game(
                        GameCommand.Create(
                            listOf(Color.RED, Color.GREEN, Color.BLUE), 3, r
                        )
                    )
                }.bind()
                mapOf("id" to id.value.toString())
            }
        }
    }
}
