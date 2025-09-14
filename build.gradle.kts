val group: String by project
val version: String by project
val minecraftVersion: String by project
val jdkVersion: String by project
val kotlinVersion: String by project

project.group = group
project.version = version

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.shadow)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
}

dependencies {
    compileOnly(libs.kotlin)
    compileOnly(libs.paper)
}

kotlin {
    jvmToolchain(
        libs.versions.jdk
            .get()
            .toInt(),
    )
}

tasks.jar {
    finalizedBy("shadowJar")
    enabled = false
}

tasks.processResources {
    val minecraftVersion =
        libs.versions.paper
            .get()
            .substringBefore("-")


    val props =
        mapOf(
            "NAME" to project.name,
            "VERSION" to project.version,
            "MINECRAFT_VERSION" to minecraftVersion,
            "KOTLIN_VERSION" to kotlinVersion,
            "KOTLIN_VERSION" to libs.versions.kotlin.get(),
        )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveFileName.set("${project.name}-${project.version}.jar")

    if (System.getenv("TEST_PLUGIN_BUILD") != null) {
        val serverPath = System.getenv("SERVER_PATH")
        if (serverPath != null) {
            destinationDirectory.set(file("$serverPath\\plugins"))
        } else {
            logger.warn("SERVER_PATH property is not set!")
        }
    }
}
