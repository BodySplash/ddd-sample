plugins {
    id("bodysplash.kotlin-library-conventions")
}

dependencies {
    implementation(project(":lib:common"))
    implementation(project(":lib:runtime"))
    api(libs.jooq.core)
    api(libs.postgres)
    api(libs.hikari)
    implementation(libs.flyway)
    implementation(libs.flyway.postgres)
    implementation(libs.typesafe.config)
    implementation(libs.kotlinx.serialization.hocon)
}