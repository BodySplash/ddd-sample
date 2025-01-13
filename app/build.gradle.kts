plugins {
    id("bodysplash.kotlin-library-conventions")
    alias(libs.plugins.ktor)
    alias(libs.plugins.ksp)
}


val development  by extra(true)

application {
    mainClass.set("bodysplash.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.bundles.runtime)
    implementation(libs.koin.core)


    ksp(libs.koin.ksp.compiler)
    implementation(libs.koin.ksp.compiler)
    implementation(libs.koin.annotations.core)

    implementation(project(":lib:common"))
    implementation(project(":lib:runtime"))
    implementation(project(":lib:ddd"))
    implementation(project(":lib:web"))
    implementation(project(":lib:eventsourcing"))

    testImplementation(libs.bundles.unittest.kotlin)

}