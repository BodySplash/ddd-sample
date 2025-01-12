package lib.eventsourcing

import arrow.atomic.AtomicBoolean
import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.resilience.SagaScope
import arrow.resilience.saga
import arrow.resilience.transact
import com.google.common.flogger.FluentLogger
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import lib.runtime.Agent
import org.postgresql.PGNotification
import org.postgresql.jdbc.PgConnection
import org.postgresql.util.PSQLException
import org.postgresql.util.PSQLState
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource
import kotlin.use

class NotificationListener(
    private val ds: DataSource,
    val schema: String,
    private val timeout: Int = 10000
) : Agent {

    override val name: String = "NotificationListener"

    override suspend fun onStart() {
        isRunning.set(true)
    }

    override suspend fun onStop() {
        publishers.forEach { (_, v) -> v.close() }
        connectionState.close()
    }

    override suspend fun doWork() {
        ensureConnection()
        tryGetNotif(connectionState).groupBy { it.name }
            .forEach { (key, events) ->
                LOGGER.atFine().log("Received %s events for %s", events.size, key)
                publishers[key]?.publish(events)
            }
    }

    internal suspend fun ensureConnection() {
        connectionState =
            connectionState.ensureConnection(ds, publishers.keys, this@NotificationListener::prime)
    }

    private suspend fun tryGetNotif(connectionState: ConnectionState): List<PGNotification> {
        return connectionState.getNotifications(timeout)
            .fold({
                LOGGER.atWarning().withCause(it).log("Failed to get notifications")
                connectionState.close()
                this@NotificationListener.connectionState = ConnectionState.NotConnected
                emptyList()
            }, { it })

    }

    private suspend fun prime() {
        publishers.forEach { (_, v) -> v.prime() }
    }

    fun <T> registerListener(
        channel: String,
        priming: suspend () -> Unit,
        converter: (p: List<String>) -> T
    ): ReceiveChannel<T> {
        if (isRunning.get()) {
            throw IllegalStateException("Listener already started")
        }
        val qualifiedName = "${schema}_$channel"
        val publisher = NotificationPublisher(converter, priming)
        publishers[qualifiedName] = publisher
        return publisher.channel
    }

    private var connectionState: ConnectionState = ConnectionState.NotConnected
    private val publishers: ConcurrentHashMap<String, NotificationPublisher<*>> = ConcurrentHashMap()
    private val isRunning = AtomicBoolean(false)

    companion object {
        internal val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()
    }

    private inner class NotificationPublisher<T>(
        val converter: (p: List<String>) -> T,
        val priming: suspend () -> Unit
    ) {
        suspend fun publish(events: List<PGNotification>) {
            channel.send(converter(events.map { it.parameter }))
        }

        suspend fun prime() {
            priming()
        }

        fun close() {
            channel.close()
        }

        var channel: Channel<T> = Channel()
    }
}

private sealed interface ConnectionState {

    suspend fun ensureConnection(
        ds: DataSource,
        qualifiedNames: Set<String>,
        prime: suspend () -> Unit,
    ): ConnectionState

    suspend fun getNotifications(timeout: Int): Either<PSQLException, List<PGNotification>>
    fun close()

    data object NotConnected : ConnectionState {
        override suspend fun ensureConnection(
            ds: DataSource,
            qualifiedNames: Set<String>,
            prime: suspend () -> Unit
        ): ConnectionState = withContext(Dispatchers.IO) {
            val saga = saga {
                val connection = pgConnection(ds)
                prime()
                startListening(qualifiedNames, connection)
                connection
            }
            try {
                Connected(saga.transact())
            } catch (e: Exception) {
                NotificationListener.LOGGER.atSevere().withCause(e).log("Error starting listener")
                this@NotConnected
            }
        }

        private fun startListening(
            qualifiedNames: Set<String>,
            connection: PgConnection
        ) {
            qualifiedNames.forEach { qualifiedName ->
                connection.createStatement()
                    .use { statement -> statement.execute("LISTEN \"$qualifiedName\"") }
                NotificationListener.LOGGER.atInfo().log("Listening to %s for new events", qualifiedName)
            }

            if (!connection.autoCommit) {
                connection.commit()
            }
        }

        private suspend fun SagaScope.pgConnection(ds: DataSource): PgConnection =
            saga({
                val connection = ds.connection.unwrap(PgConnection::class.java)
                if (ds.isWrapperFor(HikariDataSource::class.java)) {
                    ds.unwrap(HikariDataSource::class.java).apply {
                        evictConnection(connection)
                    }
                }
                connection
            }, { connection ->
                connection.close()
            })

        override suspend fun getNotifications(timeout: Int): Either<PSQLException, List<PGNotification>> {
            delay(timeout.toLong())
            return Either.Right(emptyList())
        }

        override fun close() {

        }

    }

    data class Connected(private val connection: PgConnection) : ConnectionState {
        override suspend fun ensureConnection(
            ds: DataSource,
            qualifiedNames: Set<String>,
            prime: suspend () -> Unit
        ): ConnectionState = this

        override suspend fun getNotifications(timeout: Int): Either<PSQLException, List<PGNotification>> =
            withContext(Dispatchers.IO) {
                either {
                    ensure(connection.isValid(2)) {
                        PSQLException(
                            "Connection is not valid",
                            PSQLState.CONNECTION_FAILURE
                        )
                    }
                    catch({
                        val notifications: Array<out PGNotification> =
                            connection.getNotifications(timeout)
                                ?: return@catch emptyList()
                        listOf(*notifications)
                    }) { it: PSQLException ->
                        raise(it)
                    }
                }

            }

        override fun close() {
            connection.close()
        }

    }

}
