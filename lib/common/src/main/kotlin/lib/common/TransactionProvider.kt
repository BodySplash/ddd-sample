package lib.common

interface TransactionProvider {
    suspend fun <T> withTransaction(isolated: Boolean = false, work: suspend () -> T): T
}