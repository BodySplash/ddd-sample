package lib.database.jooq

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.future.await
import kotlinx.coroutines.stream.consumeAsFlow
import org.jooq.Field
import org.jooq.Record
import org.jooq.ResultQuery
import org.jooq.exception.TooManyRowsException
import kotlin.reflect.KClass


suspend fun <T : Record> ResultQuery<T>.fetchOneAsync(): T? {
    val result = fetchAsync().await()
    if (result.isNotEmpty && result.size > 1) {
        throw TooManyRowsException("Query returned more than one result")
    }
    return result.firstOrNull()
}

suspend fun <T : Record, R> ResultQuery<T>.fetchOneAsync(mapper: (T) -> R): R? = fetchOneAsync()?.let(mapper)

suspend fun <T : Record, R> ResultQuery<T>.fetchOneAsync(field: Field<R>): R? = fetchOneAsync { it[field] }

suspend fun <T : Record> ResultQuery<T>.fetchManyAsync(): List<T> = fetchAsync().await()

suspend fun <T : Record, O : Any> ResultQuery<T>.fetchIntoAsync(type: KClass<O>): List<O> =
    fetchAsync().await().into(type.java)

suspend fun <T : Record, R> ResultQuery<T>.fetchManyAsync(mapper: (T) -> R): List<R> = fetchManyAsync().map(mapper)

suspend fun <T : Record, R> ResultQuery<T>.fetchManyAsync(field: Field<R>): List<R> = fetchManyAsync { it[field] }

fun <T : Record> ResultQuery<T>.flow(batchSize: Int = 300): Flow<T> =
    fetchSize(batchSize).fetchStream().consumeAsFlow().flowOn(Dispatchers.IO)