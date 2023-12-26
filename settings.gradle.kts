pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "NeoForged"
            setUrl("https://maven.neoforged.net/releases")
        }
        maven {
            setUrl("https://repo.spongepowered.org/repository/maven-public/")
        }
        maven {
            name = "Garden of Fancy"
            setUrl("https://maven.gofancy.wtf/releases")
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.5.0")
}
rootProject.name = "CubicChunks"
includeBuild("CubicChunksAPI") {
    dependencySubstitution {
        substitute(module("io.github.opencubicchunks:cubicchunks-api")).using(project(":"))
    }
}