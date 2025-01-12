package bodysplash

import arrow.continuations.SuspendApp
import arrow.fx.coroutines.resourceScope
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.awaitCancellation
import lib.web.ktorServer
import org.koin.ktor.plugin.Koin
import org.koin.logger.SLF4JLogger

fun main() = SuspendApp {
    resourceScope {
        ktorServer(embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module))
        awaitCancellation()
    }
}

fun Application.module() {
    install(Koin) {
        logger(SLF4JLogger())
        modules(applicationModule)
    }
    install(ContentNegotiation) {
        json()
    }
    configureRouting()
}
