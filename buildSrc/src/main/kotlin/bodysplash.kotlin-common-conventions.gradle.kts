plugins {
    id("bodysplash.common-conventions")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}


