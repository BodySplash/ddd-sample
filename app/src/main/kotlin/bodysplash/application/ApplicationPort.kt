package bodysplash.application

import bodysplash.domain.Repositories
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import lib.ddd.domain.ReplyConsumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ApplicationCommand

class ApplicationPort(val repositories: Repositories) {

    suspend fun handle(command: ApplicationCommand) {

    }
}

suspend fun <Reply> ApplicationPort.ask(
    timeout: Duration = 10.seconds,
    block: (ReplyConsumer<Reply>) -> ApplicationCommand
): Reply {
    val deferred: CompletableDeferred<Reply> = CompletableDeferred()
    handle(block(deferred::complete))
    return withTimeout(timeout) {
        deferred.await()
    }
}

