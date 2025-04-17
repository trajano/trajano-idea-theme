import blue.endless.jankson.JsonGrammar
import coffee.cypher.json_processor.*
plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("coffee.cypher.json-processor") version "0.1.0"
//    kotlin("jvm") version "1.9.22" // Optional, remove if not using Kotlin
}

group = "net.trajano"
version = "1.0.0"
//
//intellij {
//    version.set("2023.3") // Change as needed
//    type.set("IC")        // IC = IntelliJ Community, IU = Ultimate
//    plugins.set(listOf()) // You can add dependencies like "java", "Kotlin", etc.
//}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}
dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
    }
}

tasks {
    patchPluginXml {
//        sinceBuild.set("232")
//        untilBuild.set("241.*")
    }

    runIde {
//        ideDirectory.set(file("/path/to/your/intellij")) // Optional: specify a local IDE
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Remove original .json5 from being copied as-is
//    exclude("**/*.json5")

    from("src/main/resources") {
        include("**/*.json5")

        eachFile {
            // Rename: theme.json5 → theme.json
            name = name.removeSuffix(".json5") + ".json"

            processJson {
                outputFormat = JsonGrammar.STRICT
            }
        }
    }
}
