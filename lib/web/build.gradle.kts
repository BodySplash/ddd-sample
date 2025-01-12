plugins {
    id("bodysplash.kotlin-library-conventions")
}

dependencies {
    implementation(platform(libs.koin.bom))
    implementation(platform(libs.koin.annotations.bom))
    implementation(project(":lib:common"))
    implementation(libs.kotlinx.serialization.json)
    api(libs.bundles.ktor)
}