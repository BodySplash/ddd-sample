package lib.database.jooq

import kotlinx.coroutines.future.await
import org.jooq.Batch

suspend fun Batch.await(): IntArray =
    executeAsync().await()