package bodysplash.application

import bodysplash.domain.GameCommand
import bodysplash.domain.GameId
import bodysplash.domain.Repositories
import lib.ddd.domain.ReplyConsumer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ApplicationCommand {
    data class Game(val id: GameId, val command: GameCommand) : ApplicationCommand
}

class ApplicationPort(val repositories: Repositories) {

    suspend fun handle(command: ApplicationCommand) {
        when (command) {
            is ApplicationCommand.Game -> doHandle(command)
        }
    }

    private suspend fun doHandle(message: ApplicationCommand.Game) {
        repositories.games.get(message.id).handle(message.command)
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

suspend fun <Reply> ApplicationPort.askGame(
    id: GameId,
    timeout: Duration = 10.seconds,
    block: (ReplyConsumer<Reply>) -> GameCommand) =  ask(timeout) { r -> ApplicationCommand.Game(id, block(r)) }
