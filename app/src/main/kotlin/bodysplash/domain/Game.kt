package bodysplash.domain

import lib.ddd.domain.BusinessResult
import lib.ddd.domain.ReplyConsumer
import java.util.*

@JvmInline
value class GameId(val value: UUID) {
    companion object
}

enum class Color

sealed interface GameCommand {

    data class Initialise(
        val maxGuesses: Int,
        val color: List<Color>,
        val replyTo: ReplyConsumer<BusinessResult<GameId>>
    ): GameCommand
}

sealed interface GameState

sealed interface GameEvent

object GameBehaviour : Aggregate<GameId, GameCommand, GameState, GameEvent> {
    override fun initialState(): GameState {
        TODO("Not yet implemented")
    }

    override fun decide(
        id: GameId,
        command: GameCommand,
        state: GameState
    ): GameEvent {
        TODO("Not yet implemented")
    }

    override fun evolve(
        state: GameState,
        event: GameEvent
    ): GameState {
        TODO("Not yet implemented")
    }

}