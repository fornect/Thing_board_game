plugins {
    kotlin("jvm") version "2.0.21"
    application
}

group = "com.thinggame"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}