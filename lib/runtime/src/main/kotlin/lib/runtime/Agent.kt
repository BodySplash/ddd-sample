package lib.runtime


import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import com.google.common.flogger.FluentLogger
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

interface Agent {
    suspend fun onStart() {}

    suspend fun doWork()

    suspend fun onStop() {}

    val name: String
}

sealed interface SpinningStrategy {
    data object None : SpinningStrategy

    data class Delay(val duration: Duration) : SpinningStrategy
}

data class AgentRunnerConfiguration(
    val context: CoroutineContext = Dispatchers.Default,
    val spinningStrategy: SpinningStrategy = SpinningStrategy.None
) {
    fun withContext(context: CoroutineContext) = copy(context = context)

    fun withSpinningStrategy(strategy: SpinningStrategy) = copy(spinningStrategy = strategy)

}


class AgentRunner(
    private val agent: Agent,
    val configuration: AgentRunnerConfiguration
) : SideEffectContainer<Job> {

    override fun create(): Resource<Job> {
        return resource({
            LOGGER.atInfo().log("Starting %s", agent.name)
            agent.onStart()

            val handler = CoroutineExceptionHandler { _, err ->
                LOGGER.atSevere()
                    .atMostEvery(5, TimeUnit.SECONDS)
                    .withCause(err).log("Error in ${agent.name}")
            }
            loop(CoroutineScope(coroutineContext() + handler), handler)
        }, { job, _ ->
            LOGGER.atInfo().log("Stopping %s", agent.name)
            withContext(NonCancellable) {
                agent.onStop()
            }
            job.cancelAndJoin()
        })
    }

    private fun loop(scope: CoroutineScope, handler: CoroutineExceptionHandler) = with(scope) {
        launch {
            val supervisor = CoroutineScope(currentCoroutineContext() + job() + handler)
            while (true) {
                supervisor.launch(handler) {
                    agent.doWork()
                }.join()

                configuration.spinningStrategy.wait()
            }
        }
    }

    private fun coroutineContext() = EmptyCoroutineContext


    private fun job() = SupervisorJob()

    private suspend fun SpinningStrategy.wait() = when (this) {
        is SpinningStrategy.Delay -> delay(duration)
        is SpinningStrategy.None -> yield()
    }


    companion object {
        private val LOGGER = FluentLogger.forEnclosingClass()
    }

}