package bodysplash

import bodysplash.application.ApplicationPort
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject


fun Application.configureRouting() {

    val applicationPort by inject<ApplicationPort>()

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

    }
}
