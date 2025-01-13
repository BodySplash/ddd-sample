package lib.database.configuration

import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.Serializable

@Serializable
data class DbConfiguration(
    val host: String,
    val port: Int,
    val database: String,
    val schema: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int,
    val autoTransaction: Boolean,
    val ipType: String,
) {
    fun createDataSource(): HikariDataSource {
        return createStandardDataSource()
    }

    private fun createStandardDataSource(): HikariDataSource {
        val dataSource = HikariDataSource()
        dataSource.jdbcUrl = connectionString()
        dataSource.username = username
        dataSource.schema = schema
        dataSource.password = password
        dataSource.isAutoCommit = false
        dataSource.maximumPoolSize = maxPoolSize
        return dataSource
    }

    private fun connectionString(): String = "jdbc:postgresql://${host}:$port/$database"
}
