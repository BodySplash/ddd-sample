buildscript {
    dependencies {
        classpath(libs.bundles.flyway)
        classpath(libs.postgres)
    }
    repositories {
        mavenCentral()
    }
}

plugins {
    java
}

val buildableProjects = subprojects.filter {
    it.name.startsWith("lib").not()

}

configure(buildableProjects) {
    apply(plugin = "java")
    group = "bodysplash"
    version = "0.0.1"
    dependencies {
        implementation(platform(rootProject.libs.koin.bom))
        implementation(platform(rootProject.libs.koin.annotations.bom))
    }
    repositories {
        mavenCentral()
    }
}





