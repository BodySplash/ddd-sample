package org.jooq.impl

import kotlinx.coroutines.currentCoroutineContext
import org.jooq.DSLContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

data class CurrentTransaction(val tx: DSLCoroutineContext) : AbstractCoroutineContextElement(CurrentTransaction) {
    companion object Key : CoroutineContext.Key<CurrentTransaction>
}

fun CoroutineContext.currentDslOrDefault(default: DSLContext): DSLContext = get(CurrentTransaction)?.tx?.dsl ?: default

fun CoroutineContext.currentDsl(): DSLContext =
    get(CurrentTransaction)?.tx?.dsl ?: throw IllegalStateException("No transaction found")

suspend fun <T> DSLContext.suspendingTransactionResult(
    isolated: Boolean = false,
    block: suspend DSLContext.() -> T
): T {
    val currentContext = currentCoroutineContext()
    val dsl = (if (isolated) this@suspendingTransactionResult
    else currentContext.currentDslOrDefault(this@suspendingTransactionResult))
    return TransactionImpl(dsl).wrap(block)
}
