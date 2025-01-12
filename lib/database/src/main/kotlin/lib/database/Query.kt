package lib.database

import kotlinx.coroutines.future.await
import org.jooq.Query

suspend inline fun Query.await(): Int =
    executeAsync().await()