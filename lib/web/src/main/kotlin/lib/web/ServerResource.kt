package lib.web

import arrow.fx.coroutines.continuations.ResourceScope
import io.ktor.server.engine.EmbeddedServer
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

suspend fun ResourceScope.ktorServer(
    application: EmbeddedServer<*, *>,
    preWait: Duration = 10.seconds,
    grace: Duration = 15.seconds,
    timeout: Duration = 16.seconds,
) = install({ application.apply { start() } })
{ server, _ ->

    if (!server.application.developmentMode) {
        server.environment.log.info(
            "prewait delay of ${preWait.inWholeMilliseconds}ms, turn it off using io.ktor.development=true"
        )
        delay(preWait.inWholeMilliseconds)
    }
    server.environment.log.info("Shutting down HTTP server...")
    server.engine.stop(grace.inWholeMilliseconds, timeout.inWholeMicroseconds)
    server.environment.log.info("HTTP server shutdown!")
}