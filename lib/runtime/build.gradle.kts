plugins {
    id("bodysplash.kotlin-library-conventions")
}

dependencies {
    implementation(project(":lib:common"))
    api(libs.koin.core)
    api(libs.typesafe.config)
}