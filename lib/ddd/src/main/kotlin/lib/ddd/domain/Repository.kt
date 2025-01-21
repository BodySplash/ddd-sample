package lib.ddd.domain

interface Repository<ID, Command> {
    suspend fun get(id: ID): AggregateRef<Command>
}

