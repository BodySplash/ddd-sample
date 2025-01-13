plugins {
    id("bodysplash.kotlin-library-conventions")
    alias(libs.plugins.jooq)
    alias(libs.plugins.flyway)
}

val schemaConfiguration by configurations.creating
java {
    sourceSets {
        val schema by creating {
            java.srcDir("src/generated")
            compileClasspath += schemaConfiguration
        }
    }
}

dependencies {
    schemaConfiguration(libs.jooq.core)
    api(sourceSets["schema"].output)
    implementation(project(":lib:common"))
    implementation(project(":lib:ddd"))
    api(project(":lib:database"))
    implementation(project(":lib:runtime"))
    implementation(libs.kotlinx.serialization.json)
    jooqGenerator(libs.jooq.meta.extensions)
    jooqGenerator(libs.jooq.core)
    jooqGenerator(libs.postgres)
}

val jooqDb = mapOf(
    "url" to "jdbc:postgresql://localhost:5433/ddd",
    "schema" to "event_store_schema",
    "user" to "ddd",
    "password" to "sample",
)

tasks.register("updateEventStore") {
    description = "Migrate and generate jooq"
    dependsOn("flywayClean", "flywayMigrate", "generateEventstoreJooq")
}

flyway {
    url = jooqDb["url"]
    user = jooqDb["user"]
    password = jooqDb["password"]
    schemas = arrayOf(jooqDb["schema"])
    locations = arrayOf("filesystem:src/main/resources/migration")
    cleanDisabled = false
}

jooq {
    version.set(libs.jooq.core.get().version)
    configurations {
        create("eventstore") {
            generateSchemaSourceOnCompilation.set(false)
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                jdbc.apply {
                    driver = "org.postgresql.Driver"
                    url = jooqDb["url"]
                    user = jooqDb["user"]
                    password = jooqDb["password"]
                }
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = jooqDb["schema"]
                        excludes = "flyway_schema_history*"
                    }

                    generate.apply {
                        isDeprecated = false
                        isDeprecationOnUnknownTypes = false
                        isRecords = true
                        isImmutablePojos = false
                        isPojos = false
                        isFluentSetters = true
                        isRelations = true
                    }
                    target.apply {
                        packageName = "lib.eventsourcing.schema"
                        directory = "src/generated"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}