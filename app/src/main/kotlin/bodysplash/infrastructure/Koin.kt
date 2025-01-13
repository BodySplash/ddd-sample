package bodysplash.infrastructure

import bodysplash.application.ApplicationPort
import bodysplash.domain.Repositories
import lib.common.TransactionProvider
import lib.database.configuration.jooqModule
import lib.ddd.persistence.EventStore
import lib.ddd.persistence.EventStoreMemory
import lib.eventsourcing.eventSourcingModule
import lib.eventsourcing.schema.EventStoreSchema
import lib.runtime.SideEffectContainer
import lib.runtime.SideEffects
import org.jooq.conf.MappedSchema
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


fun applicationModule() = module {
    singleOf(::EventSourcedRepositories) {
        bind<Repositories>()
    }
    singleOf(::ApplicationPort)
}

val sideEffectModule = module {
    single {
        SideEffects(getAll<SideEffectContainer<*>>())
    }
}

fun persistedModule(hikari: Module) = module {
    includes(
        hikari,
        jooqModule(MappedSchema().withInput(EventStoreSchema.EVENT_STORE_SCHEMA.name)),
        eventSourcingModule(),
        sideEffectModule,
    )
}

val inMemoryModule = module {
    singleOf(::EventStoreMemory) {
        bind<EventStore>()
    }
    single {
        object : TransactionProvider {
            override suspend fun <T> withTransaction(isolated: Boolean, work: suspend () -> T): T = work()

        } as TransactionProvider
    }
    single {
        SideEffects(emptyList())
    }
}