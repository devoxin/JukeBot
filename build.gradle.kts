@file:Suppress("DEPRECATION")

import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

group = "me.devoxin.jukebot"
version = buildCommitHash
setProperty("mainClassName", "$group.Launcher")

buildscript {
    repositories {
        jcenter()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    java
    application
    kotlin("jvm") version(libs.versions.kotlin.get())
    id("com.github.johnrengelman.shadow") version("8.1.1")
}

java {
    sourceCompatibility = VERSION_11
    targetCompatibility = VERSION_11
}

kotlin {
    compilerOptions.jvmTarget = JvmTarget.JVM_11
}

repositories {
    maven {
        name = "m2-dv8tion"
        setUrl("https://m2.dv8tion.net/releases")
    }
    mavenCentral()
    jcenter()
    //maven { url "https://dl.bintray.com/natanbc/maven" }
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    val udpqueueModules = setOf(
        "linux-x86-64", "linux-x86", "linux-aarch64", "linux-arm", "linux-musl-x86-64", "linux-musl-aarch64",
        "win-x86-64", "win-x86",
        "darwin"
    )

    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(module = "opus-java")
    }

    implementation("com.github.devoxin:flight:572acd6")

    // Audio
    implementation("com.github.devoxin.lavaplayer:lavaplayer:${libs.versions.lavaplayer.get()}")
    implementation("com.github.devoxin.lavaplayer:lavaplayer-ext-youtube-rotator:${libs.versions.lavaplayer.get()}")
    implementation("com.sedmelluq:jda-nas:1.1.0") {
        exclude(module = "udp-queue")
    }

    udpqueueModules.forEach {
        implementation("club.minnced:udpqueue-native-$it:${libs.versions.udpqueue.get()}")
    }

    // Audio Filters
    implementation("com.github.devoxin:LavaDSPX:2.0.1")
    implementation("com.github.natanbc:lavadsp:0.7.7")

    // Database
    implementation("org.xerial:sqlite-jdbc:3.43.2.1")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("redis.clients:jedis:5.0.0")

    // Logging
    implementation("org.apache.logging.log4j:log4j-core:${libs.versions.logger.get()}")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:${libs.versions.logger.get()}")
    implementation("io.sentry:sentry:1.7.30")

    // stdlib (Kotlin)
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${libs.versions.coroutines.get()}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:${libs.versions.coroutines.get()}")

    // Utilities
    implementation("com.grack:nanojson:1.7")
    implementation("org.jsoup:jsoup:1.16.2")
    implementation("commons-cli:commons-cli:1.6.0")

    // Transitive overrides
    implementation("org.json:json:20231013")
    implementation("commons-codec:commons-codec:1.13")
    implementation("com.squareup.okio:okio-jvm:3.4.0")
}

tasks.register("writeVersion") {
    val version = file("src/main/resources/version.txt")
    version.parentFile.mkdirs()
    version.writeText(buildCommitHash, StandardCharsets.UTF_8)
}

tasks.shadowJar {
    archiveFileName = "JukeBot.jar"
//    from("src/main/Resources")

    manifest {
        attributes["Main-Class"] = "me.devoxin.jukebot.Launcher"
    }

    dependsOn(tasks["writeVersion"])
}

val buildCommitHash: String
    get() = ByteArrayOutputStream().use {
        exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = it
        }

        it.toString().trim()
    }
