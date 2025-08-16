group = "ru.joutak"
version = System.getProperty("pluginVersion")
val commitHash = System.getProperty("commitHash")
if (commitHash.isNotBlank()) {
    version = "$version-$commitHash"
}

plugins {
    kotlin("jvm") version (System.getProperty("kotlinVersion"))
    id("com.github.johnrengelman.shadow") version ("8.1.1")
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
    compileOnly("io.papermc.paper:paper-api:${System.getProperty("minecraftVersion")}-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib")
}

kotlin {
    jvmToolchain(System.getProperty("javaVersion").toInt())
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName.set("${project.name}.jar")

    val serverPath = System.getenv("SERVER_PATH")
    if (System.getenv("TESTING") != null) {
        if (serverPath != null) {
            destinationDirectory.set(file("$serverPath\\plugins"))
        } else {
            logger.warn("SERVER_PATH property is not set!")
        }
    }
}

tasks.jar {
    finalizedBy("shadowJar")
    enabled = false
}

tasks.processResources {
    val props =
        mapOf(
            "pluginVersion" to version,
            "pluginName" to project.name,
            "minecraftVersion" to System.getProperty("minecraftVersion"),
            "kotlinVersion" to System.getProperty("kotlinVersion"),
        )
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
