package lib.runtime

import arrow.fx.coroutines.resource
import com.google.common.flogger.FluentLogger

class SideEffects(private val containers: Set<SideEffectContainer<*>>) {

    suspend fun asResource() = resource {
        LOGGER.atInfo().log("Starting side effects ${containers.map { it::class.simpleName }}")
        containers
            .map { it.create() }
            .forEach {
                it.bind()
            }

    }

    companion object {
        val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()
    }
}
