package lib.database.configuration

import arrow.fx.coroutines.resource
import com.typesafe.config.Config
import com.zaxxer.hikari.HikariDataSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import javax.sql.DataSource

data class ConfiguredDataSource(
    val config: DbConfiguration,
    val dataSource: DataSource
)

class HikariProvider private constructor(
    private val config: Config,
) {
    fun provide(configKey: String): DataSource = provideConfigAndDataSource(configKey).dataSource

    fun provideConfig(configKey: String): DbConfiguration = provideConfigAndDataSource(configKey).config

    @OptIn(ExperimentalSerializationApi::class)
    fun provideConfigAndDataSource(configKey: String = "postgres"): ConfiguredDataSource {
        return sources.computeIfAbsent(configKey) {
            val dbConfig = Hocon.decodeFromConfig<DbConfiguration>(config.getConfig(configKey))
            val dataSource = dbConfig.createDataSource()
            ConfiguredDataSource(dbConfig, dataSource)
        }
    }

    private fun close() {
        sources.values.forEach { (_, dataSource) ->
            if (dataSource.isWrapperFor(HikariDataSource::class.java)) {
                dataSource.unwrap(HikariDataSource::class.java).close()
            }
        }
    }

    private val sources: MutableMap<String, ConfiguredDataSource> = mutableMapOf()

    companion object {
        internal fun build(config: Config) =
            resource({
                HikariProvider(
                    config,
                )
            }) { m, _ -> m.close() }
    }
}