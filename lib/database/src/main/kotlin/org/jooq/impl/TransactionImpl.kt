package org.jooq.impl

import arrow.core.Either
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jooq.DSLContext
import org.jooq.TransactionProvider
import org.jooq.exception.DataAccessException

class TransactionImpl(parentDsl: DSLContext) : DSLCoroutineContext {

    private val listeners: TransactionListeners
    private val provider: TransactionProvider
    private val ctx: DefaultTransactionContext
    override val dsl: DSLContext

    private var committed: Boolean = false
    private var rollbacked: Boolean = false


    init {
        val derivedConfiguration = parentDsl.configuration().derive()
        ctx = DefaultTransactionContext(derivedConfiguration)
        provider = ctx.configuration().transactionProvider()
        listeners = TransactionListeners(ctx.configuration())
        dsl = DSL.using(derivedConfiguration)
    }

    suspend fun start() = withContext(Dispatchers.IO) {
        try {
            listeners.beginStart(ctx)
            provider.begin(ctx)
        } finally {
            listeners.beginEnd(ctx)
        }
    }

    suspend fun <T> wrap(block: suspend (dsl: DSLContext) -> T): T {
        try {
            start()
            val result = withContext(CurrentTransaction(this)) { block(dsl) }
            return commit(result)
        } catch (cause: Throwable) {
            rollback(cause)
            when (cause) {
                is RuntimeException, is Error -> throw cause
                else -> throw DataAccessException(
                    if (committed) "Exception after commit" else "Rollback caused",
                    cause
                )
            }
        }
    }

    suspend fun <T> commit(result: T): T {
        if (result is Either<*, *>) {
            result.fold(
                {
                    if (it is Throwable) {
                        rollback(it)
                        rollbacked = true
                    }
                },
                {
                    withContext(Dispatchers.IO) {
                        try {
                            listeners.commitStart(ctx)
                            provider.commit(ctx)
                            committed = true
                        } finally {
                            listeners.commitEnd(ctx)
                        }
                    }
                }
            )
        } else {
            withContext(Dispatchers.IO) {
                try {
                    listeners.commitStart(ctx)
                    provider.commit(ctx)
                    committed = true
                } finally {
                    listeners.commitEnd(ctx)
                }
            }
        }
        return result
    }

    suspend fun rollback(
        cause: Throwable?,
    ) {
        // [#8413] Avoid rollback logic if commit was successful (exception in commitEnd())
        if (!committed && !rollbacked) {
            if (cause is Exception) {
                ctx.cause(cause)
            } else {
                ctx.causeThrowable(cause)
            }

            withContext(Dispatchers.IO) {
                listeners.rollbackStart(ctx)
                try {
                    provider.rollback(ctx)
                } catch (suppress: Exception) {
                    cause?.addSuppressed(suppress)
                }
                listeners.rollbackEnd(ctx)
            }
        }
    }
}