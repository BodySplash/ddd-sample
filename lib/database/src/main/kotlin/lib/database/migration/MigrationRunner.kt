package lib.database.migration

import arrow.fx.coroutines.Resource
import arrow.fx.coroutines.resource
import com.google.common.flogger.FluentLogger
import lib.database.configuration.ConfiguredDataSource
import lib.database.configuration.DbConfiguration
import lib.runtime.SideEffectContainer
import org.flywaydb.core.Flyway
import javax.sql.DataSource

class MigrationRunner(
    private val name: String,
    private val ds: DataSource,
    private val config: DbConfiguration,
    private val locations: String = "db/migration",
    private val historyTable: String = "flyway_schema_history",
) :
    SideEffectContainer<Unit> {

    companion object {
        val LOGGER: FluentLogger = FluentLogger.forEnclosingClass()
    }

    constructor(
        name: String,
        db: ConfiguredDataSource,
        locations: String = "db/migration",
        historyTable: String = "flyway_schema_history"
    ) : this(name, db.dataSource, db.config, locations, historyTable)

    override fun create(): Resource<Unit> = resource {
        install({

            val schema: String = config.schema
            val configuration = Flyway
                .configure()
                .locations(locations)
                .schemas(schema)
                .baselineVersion("0")
                .placeholders(mapOf("schema" to schema))
                .baselineOnMigrate(true)
                .table(historyTable)
                .dataSource(ds)
            LOGGER.atInfo().log("Migrating $name")

            val migrate = Flyway(configuration).migrate()

            LOGGER.atInfo().log(
                "$name migration done. From %s to  %s. Applied %s updates",
                migrate.initialSchemaVersion,
                migrate.targetSchemaVersion,
                migrate.migrations.size,
            )
        }, { _, _ -> })
    }

}