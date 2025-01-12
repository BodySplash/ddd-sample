package lib.database

import org.jooq.DSLContext
import org.jooq.Query
import kotlin.collections.isNotEmpty

fun interface QueryAdder {
    suspend fun add(query: Query)
}

class SqlBatchManager(private val batchSize: Int = 100) {

    suspend fun withDsl(block: QueryAdder.(dsl: DSLContext) -> Query) {
        block.invoke(::add, currentTransaction())
    }

    suspend fun add(query: Query) {
        currentBatch.add(query)
        if (currentBatch.size >= batchSize) {
            flush()
        }
    }

    suspend fun flush() {
        if (currentBatch.isNotEmpty()) {
            currentTransaction().batch(currentBatch).await()
            currentBatch.clear()
        }
    }

    private val currentBatch: MutableList<Query> = mutableListOf()
}