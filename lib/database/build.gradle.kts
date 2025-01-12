plugins {
    id("bodysplash.kotlin-library-conventions")
}

dependencies {
    implementation(project(":lib:common"))
    implementation(libs.jooq.core)
}