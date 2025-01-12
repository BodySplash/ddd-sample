package bodysplash.application

import bodysplash.domain.GameCommand
import bodysplash.domain.GameId
import bodysplash.domain.Repositories
import bodysplash.support.ReplyConsumer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ApplicationCommand {
    data class Game(val command: GameCommand) : ApplicationCommand
}

class ApplicationPort(val repositories: Repositories) {

    suspend fun handle(command: ApplicationCommand) {
        when (command) {
            is ApplicationCommand.Game -> doHandle(command)
        }
    }

    private suspend fun doHandle(message: ApplicationCommand.Game) {
        repositories.games.get(GameId(UUID.randomUUID())).handle(message.command)
    }
}

suspend fun <Reply> ApplicationPort.ask(
    timeout: Duration = 10.seconds,
    block: (ReplyConsumer<Reply>) -> ApplicationCommand
): Reply {
    val deferred: CompletableDeferred<Reply> = CompletableDeferred()
    handle(block { message -> deferred.complete(message) })
    return withTimeout(timeout) {
        deferred.await()
    }
}