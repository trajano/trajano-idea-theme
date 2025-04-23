import blue.endless.jankson.JsonGrammar
import coffee.cypher.json_processor.*
import org.jetbrains.intellij.platform.gradle.utils.rootProjectPath

plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
    id("coffee.cypher.json-processor") version "0.1.0"
    kotlin("jvm") version "2.1.20"

}

group = "net.trajano"
version = "1.0.0"
//
//intellij {
//    version.set("2023.3") // Change as needed
//    type.set("IC")        // IC = IntelliJ Community, IU = Ultimate
//    plugins.set(listOf()) // You can add dependencies like "java", "Kotlin", etc.
//}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.vladsch.flexmark:flexmark-all:0.64.0")
    }
}
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

val renderedReadmeHtml by lazy {
    val readmeFile = rootDir.resolve("README.md")
    val readmeText = readmeFile.readText()
    val options = com.vladsch.flexmark.util.data.MutableDataSet()
    val parser = com.vladsch.flexmark.parser.Parser.builder(options).build()
    val renderer = com.vladsch.flexmark.html.HtmlRenderer.builder(options).build()
    val document = parser.parse(readmeText)
    buildString {
        append(renderer.render(document))
    }
}
tasks {
    patchPluginXml {
        pluginDescription = renderedReadmeHtml
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
    from("src/main/resources") {
        include("**/*.xml")

        eachFile {
            name = name.removeSuffix(".xml") + ".icls"
        }
    }
}
