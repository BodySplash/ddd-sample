plugins {
    id("bodysplash.kotlin-library-conventions")
}

dependencies {
    implementation(project(":lib:common"))
    implementation(libs.kotlinx.serialization.json)
}