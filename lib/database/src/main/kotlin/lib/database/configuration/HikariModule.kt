package lib.database.configuration

import arrow.fx.coroutines.ResourceScope
import com.typesafe.config.Config
import org.koin.core.module.Module
import org.koin.dsl.module


suspend fun ResourceScope.hikariModule(configuration: Config, defaultKey: String = "postgres"): Module {
    val p = HikariProvider.build(configuration).bind()
    return module {
        single { p }
        single(createdAtStart = true) { p.provide(defaultKey) }
    }
}

