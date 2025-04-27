plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    application
}


repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"
val exposedVersion = "0.45.0"
val kotlinSerializationVersion = "1.6.3"
val jwtVersion = "4.4.0"
val dotenvVersion = "6.4.1"
val postgresqlVersion = "42.7.2"
val bcryptVersion = "0.4"
val logbackVersion = "1.5.13"

dependencies {
    // Ktor Core
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    // Auth
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    // Serialization
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinSerializationVersion")

    // Exposed
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    // PostgreSQL
    implementation("org.postgresql:postgresql:$postgresqlVersion")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // JWT
    implementation("com.auth0:java-jwt:$jwtVersion")

    // Bcrypt
    implementation("org.mindrot:jbcrypt:$bcryptVersion")

    // Dotenv
    implementation("io.github.cdimascio:dotenv-kotlin:$dotenvVersion")
}

application {
    mainClass.set("MainKt")
}

tasks.register<Jar>("buildFatJar") {
    archiveBaseName.set("API")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "MainKt" // Entry point of app
    }
    from(sourceSets.main.get().output)

    // Include all dependencies
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "MainKt"
        )
    }
}
