package bodysplash.domain

import com.github.f4b6a3.uuid.UuidCreator
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import lib.ddd.domain.BusinessResult
import lib.ddd.domain.FakeReply
import lib.ddd.domain.testRun
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test


fun GameId.Companion.random() = GameId(UuidCreator.getTimeOrderedEpoch())

class MastermindTests {

    @Test
    fun `should initialize`() {
        val replyTo = FakeReply<BusinessResult<GameId>>()
        val id = GameId.random()
        val colors = (1..5).map { Color.entries.random() }

        val result = Mastermind.testRun(
            id,
            GameCommand.Create(
                code = colors, maxGuesses = 5, replyTo = replyTo
            )
        )

        replyTo.last.shouldNotBeNull() shouldBeRight id
        result.events shouldContain GameEvent.Created(colors, 5)
        result.newState shouldBe GameState.InProgress(colors, 5)
    }

    @Nested
    inner class WithRunningGame {

        private val state =
            GameState.InProgress(listOf(Color.RED, Color.YELLOW, Color.RED, Color.GREEN, Color.BLACK), 5)

        @Test
        fun `should guess wrong`() {
            val replyTo = FakeReply<BusinessResult<TurnResult>>()

            val result = Mastermind.testRun(
                GameId.random(), GameCommand.Guess(
                    listOf(
                        Color.GREEN, Color.YELLOW, Color.RED, Color.GREEN, Color.BLACK
                    ),
                    replyTo
                ), state = state
            )

            result.events shouldContain GameEvent.GuessedWrong
            result.newState shouldBe state.copy(remainingGuesses = 4)
            replyTo.last.shouldNotBeNull() shouldBeRight TurnResult.TryAgain(
                mapOf(
                    GuessOutcome.ALMOST to 1,
                    GuessOutcome.CORRECT to 4
                )
            )
        }

        @Test
        fun `should guess properly`() {
            val replyTo = FakeReply<BusinessResult<TurnResult>>()

            val result = Mastermind.testRun(
                GameId.random(), GameCommand.Guess(
                    state.code,
                    replyTo
                ), state = state
            )

            result.events shouldContain GameEvent.Ended(Winner.PLAYER)
            result.newState shouldBe GameState.Finished
            replyTo.last.shouldNotBeNull() shouldBeRight TurnResult.GameOver(Winner.PLAYER)
        }

        @Test
        fun `should loose game`() {
            val replyTo = FakeReply<BusinessResult<TurnResult>>()

            val result = Mastermind.testRun(
                GameId.random(), GameCommand.Guess(
                    listOf(
                        Color.GREEN, Color.YELLOW, Color.RED, Color.GREEN, Color.BLACK
                    ),
                    replyTo
                ), state = state.copy(remainingGuesses = 1)
            )

            result.events shouldContain GameEvent.Ended(Winner.MASTER)
            result.newState shouldBe GameState.Finished
            replyTo.last.shouldNotBeNull() shouldBeRight TurnResult.GameOver(Winner.MASTER)
        }
    }
}


