package lib.database

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.future.await
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.currentDsl
import org.jooq.impl.suspendingTransactionResult


suspend fun <T> DSLContext.withTransaction(isolated: Boolean = false, work: suspend (dsl: DSLContext) -> T): T {
    return this.suspendingTransactionResult(isolated = isolated, block = work)
}

suspend fun currentTransaction(): DSLContext = currentCoroutineContext().currentDsl()

suspend fun DSLContext.notify(channel: String, payload: String) {
    fetchAsync("NOTIFY {0}, {1}", DSL.name(channel), DSL.inline(payload)).await()
}
