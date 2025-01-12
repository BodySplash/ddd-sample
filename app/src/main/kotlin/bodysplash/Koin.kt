package bodysplash

import bodysplash.application.ApplicationPort
import bodysplash.domain.Repositories
import bodysplash.infrastructure.EventSourcedRepositories
import lib.ddd.persistence.EventStore
import lib.ddd.persistence.EventStoreMemory
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module


val applicationModule = module {
    singleOf(::EventStoreMemory) {
        bind<EventStore>()
    }
    singleOf(::EventSourcedRepositories) {
        bind<Repositories>()
    }
    singleOf(::ApplicationPort)
}