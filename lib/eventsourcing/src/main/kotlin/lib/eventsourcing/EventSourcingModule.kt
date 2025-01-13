package lib.eventsourcing

import lib.database.configuration.HikariProvider
import lib.database.migration.MigrationRunner
import lib.ddd.persistence.EventStore
import lib.runtime.SideEffectContainer
import org.koin.dsl.bind
import org.koin.dsl.module

fun eventSourcingModule(configKey: String = "postgres") = module {
    single {
        val (dbConfiguration, dataSource) = get<HikariProvider>().provideConfigAndDataSource(configKey)
        MigrationRunner(
            "eventstore",
            config = dbConfiguration,
            ds = dataSource,
            locations = "migration",
        )
    } bind SideEffectContainer::class

    single {
        val config = get<HikariProvider>().provideConfig(configKey)
        SqlEventStore(get(), config.schema) as EventStore
    }
}