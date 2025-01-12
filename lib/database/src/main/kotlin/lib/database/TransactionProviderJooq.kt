package lib.database

import lib.common.TransactionProvider
import org.jooq.DSLContext

class TransactionProviderJooq(private val context: DSLContext) : TransactionProvider {
    override suspend fun <T> withTransaction(isolated: Boolean, work: suspend () -> T): T =
        context.withTransaction(isolated) { work() }

}