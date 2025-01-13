package bodysplash

import arrow.continuations.SuspendApp
import arrow.fx.coroutines.resourceScope
import bodysplash.infrastructure.applicationModule
import bodysplash.infrastructure.persistedModule
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.awaitCancellation
import lib.database.configuration.hikariModule
import lib.runtime.SideEffects
import lib.web.ktorServer
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.ktor.plugin.setKoinApplication

fun main() = SuspendApp {
    resourceScope {
        val config = lib.runtime.ApplicationConfig.load()
        val hikari = hikariModule(config)
        val koinApp = startKoin {
            modules(
                persistedModule(hikari),
                //inMemoryModule,
                applicationModule()
            )
        }
        koinApp.koin.get<SideEffects>().asResource().bind()
        ktorServer(embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = {
            module(koinApp)
        }))
        awaitCancellation()
        koinApp.close()
    }
}

fun Application.module(koinApp: KoinApplication) {
    setKoinApplication(koinApp)
    install(ContentNegotiation) {
        json()
    }
    configureRouting()
}
