package bodysplash.domain

import arrow.core.raise.ensure
import bodysplash.domain.GameState.InProgress
import bodysplash.support.AggregateBehaviour
import bodysplash.support.AggregateEffect
import bodysplash.support.ReplyConsumer
import bodysplash.support.buildEffect
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import lib.common.BusinessError
import lib.ddd.domain.BusinessResult
import java.util.*

@JvmInline
value class GameId(val value: UUID) {
    companion object
}

sealed interface GameCommand {
    data class
    Create(
        val code: List<Color>,
        val guesses: Int,
        val replyTo: ReplyConsumer<BusinessResult<GameId>>
    ) : GameCommand

    data class Guess(val colors: List<Color>, val replyTo: ReplyConsumer<BusinessResult<TurnResult>>) :
        GameCommand
}

enum class GuessOutcome { CORRECT, ALMOST }


@Serializable
sealed interface TurnResult {

    @Serializable
    @SerialName("game_over")
    data class GameOver(val winner: Winner) : TurnResult

    @Serializable
    @SerialName("try_again")
    data class TryAgain(val hints: Map<GuessOutcome, Int>) : TurnResult
}


@Serializable
sealed interface GameEvent {
    @Serializable
    data class Created(
        val code: List<Color>,
        val guesses: Int,
    ) : GameEvent

    @Serializable
    data object GuessedWrong : GameEvent

    @Serializable
    data class Ended(val winner: Winner) : GameEvent
}

enum class Color {
    WHITE, BLACK, RED, BLUE, YELLOW, GREEN
}

enum class Winner {
    MASTER, PLAYER
}

sealed interface GameState {
    data object Initial : GameState
    data class InProgress(val code: List<Color>, val guesses: Int) : GameState
    data object Finished : GameState
}

object Mastermind : AggregateBehaviour<GameId, GameCommand, GameState, GameEvent> {

    override fun initialState(): GameState = GameState.Initial

    override fun decide(
        id: GameId,
        command: GameCommand,
        state: GameState
    ): AggregateEffect<GameEvent> = when (command) {
        is GameCommand.Create -> handleCreate(id, command, state)
        is GameCommand.Guess -> handleGuess(command, state)
    }

    private fun handleGuess(
        guess: GameCommand.Guess,
        state: GameState
    ): AggregateEffect<GameEvent> = buildEffect(guess.replyTo) { builder ->
        ensure(state is InProgress) {
            BusinessError.withCode("BAD_STATE")
        }
        val result = guess.colors.mapIndexedNotNull { index, color ->
            if (state.code[index] == color) GuessOutcome.CORRECT
            else if (state.code.contains(color)) GuessOutcome.ALMOST
            else null
        }.groupBy { it }.mapValues { (_, count) -> count.size }


        if (playerWon(result, state)) {
            return@buildEffect builder.persist(GameEvent.Ended(Winner.PLAYER))
                .andReply(TurnResult.GameOver(Winner.PLAYER))

        }
        if (state.guesses == 1) return@buildEffect builder.persist(GameEvent.Ended(Winner.MASTER))
            .andReply(TurnResult.GameOver(Winner.MASTER))

        builder.persist(GameEvent.GuessedWrong).andReply(TurnResult.TryAgain(result))
    }

    private fun playerWon(
        result: Map<GuessOutcome, Int>,
        state: InProgress
    ): Boolean = result.getOrDefault(GuessOutcome.CORRECT, 0) == state.code.size

    private fun handleCreate(
        id: GameId,
        create: GameCommand.Create,
        state: GameState
    ): AggregateEffect<GameEvent> = buildEffect(create.replyTo) { builder ->
        ensure(create.code.size == 5) {
            BusinessError.withCode("BAD_COLORS")
        }
        ensure(create.guesses in 1..20) {
            BusinessError.withCode("BAD_GUESSES")
        }
        ensure(state is GameState.Initial) {
            BusinessError.withCode("BAD_STATE")
        }
        builder.persist(GameEvent.Created(create.code, create.guesses)).andReply(id)
    }

    override fun evolve(
        state: GameState,
        event: GameEvent
    ): GameState = when (event) {
        is GameEvent.Created -> InProgress(event.code, event.guesses)
        is GameEvent.GuessedWrong -> (state as InProgress).copy(guesses = (state.guesses - 1))
        is GameEvent.Ended -> GameState.Finished
    }
}