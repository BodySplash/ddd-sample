package lib.database.configuration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import lib.common.TransactionProvider
import lib.database.jooq.TransactionProviderJooq
import org.jooq.SQLDialect
import org.jooq.conf.MappedSchema
import org.jooq.conf.RenderMapping
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultConfiguration
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.util.concurrent.Executor

fun jooqModule(vararg mappings: MappedSchema, configKey: String = "postgres") = module {
    singleOf(::TransactionProviderJooq) {
        bind<TransactionProvider>()
    }
    single {
        val provider = get<HikariProvider>()
        val (config, dataSource) = provider.provideConfigAndDataSource(configKey)
        val schema = config.schema
        val jooqConfiguration = DefaultConfiguration()
            .set(SQLDialect.POSTGRES)
        jooqConfiguration.set(
            Settings().withRenderMapping(
                mappings.fold(RenderMapping()) { acc, mappedSchema ->
                    acc.withSchemata(mappedSchema.withOutput(schema))
                }
            )
        )
        jooqConfiguration.set(InstrumentedExecutor())
        jooqConfiguration.set(dataSource)
        DSL.using(jooqConfiguration)
    }
}


private class InstrumentedExecutor(private val delegate: Executor = Dispatchers.IO.asExecutor()) : Executor {
    override fun execute(command: Runnable) {
        delegate.execute {
            command.run()
        }
    }
}