plugins {
    id("org.jetbrains.intellij.platform") version "2.5.0"
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
    // Add any dependencies if needed
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
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
