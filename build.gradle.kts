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


//dependencies {
//    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
//    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
//    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
//    implementation("ch.qos.logback:logback-classic:1.5.13")
//    implementation("org.jetbrains.exposed:exposed-core:0.45.0")
//    implementation("org.jetbrains.exposed:exposed-dao:0.45.0")
//    implementation("org.jetbrains.exposed:exposed-jdbc:0.45.0")
//    implementation("org.postgresql:postgresql:42.7.2")
//    implementation("org.mindrot:jbcrypt:0.4")
//    implementation("com.auth0:java-jwt:4.4.0")
//    implementation("io.ktor:ktor-server-auth:2.3.7")
//    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
//    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
//    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
//}

application {
    mainClass.set("MainKt")
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
