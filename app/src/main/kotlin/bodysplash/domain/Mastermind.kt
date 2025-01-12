package bodysplash.domain

import arrow.core.left
import arrow.core.right
import bodysplash.support.AggregateBehaviour
import bodysplash.support.AggregateEffect
import bodysplash.support.ReplyConsumer
import bodysplash.support.andReply
import kotlinx.serialization.Serializable
import lib.common.BusinessError
import lib.ddd.domain.BusinessResult
import java.util.*

@JvmInline
value class GameId(val value: UUID)

sealed interface GameCommand {
    data class Create(
        val guess: List<Color>,
        val guesses: Short,
        val replyTo: ReplyConsumer<BusinessResult<GameId>>
    ) : GameCommand
}

@Serializable
sealed interface GameEvent {
    @Serializable
    data class Created(
        val guess: List<Color>,
        val guesses: Short,
    ) : GameEvent
}

enum class Color {
    RED, BLUE, YELLOW, GREEN
}

enum class Winner {
    MASTER, PLAYER
}

sealed interface GameState {
    data object Initial : GameState
    data class InProgress(val guess: List<Color>, val guesses: Short) : GameState
    data object Finished : GameState
}

private inline fun <reified T : GameState> GameState.ensure(
    consumer: ReplyConsumer<BusinessResult<Nothing>>,
    block: (T) -> AggregateEffect<GameEvent>
): AggregateEffect<GameEvent> = if (this is T) {
    block(this)
} else
    AggregateEffect.Reply(BusinessError.withCodeAndMessage("BAD_STATE", "bad state").left(), consumer)

object Mastermind : AggregateBehaviour<GameId, GameCommand, GameState, GameEvent> {

    override fun initialState(): GameState = GameState.Initial

    override fun decide(
        id: GameId,
        command: GameCommand,
        state: GameState
    ): AggregateEffect<GameEvent> {
        return when (command) {
            is GameCommand.Create -> handleCreate(id, command, state)
        }
    }

    private fun handleCreate(
        id: GameId,
        create: GameCommand.Create,
        state: GameState
    ): AggregateEffect<GameEvent> = state.ensure<GameState.Initial>(create.replyTo) {
        AggregateEffect.Persist<GameEvent>(GameEvent.Created(create.guess, create.guesses))
            .andReply(id.right(), create.replyTo)
    }

    override fun evolve(
        state: GameState,
        event: GameEvent
    ): GameState = when (event) {
        is GameEvent.Created -> GameState.InProgress(event.guess, event.guesses)
    }
}