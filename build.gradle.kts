plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
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

ktlint {
    version.set("1.2.0")
    debug.set(false)
    verbose.set(true)
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
    enableExperimentalRules.set(true)
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}
dependencies {
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runTests") {
    group = "verification"
    description = "Запуск всех тестов через TestRunner"
    mainClass.set("TestRunnerKt")
    classpath = sourceSets["test"].runtimeClasspath
}
