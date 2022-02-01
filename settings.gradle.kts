pluginManagement {
    repositories {
        maven {
            setUrl("https://maven.minecraftforge.net/")
        }
        gradlePluginPortal()
        mavenCentral()
        maven {
            setUrl("https://repo.spongepowered.org/repository/maven-public/")
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
            if (requested.id.id == "org.spongepowered.mixin") {
                useModule("org.spongepowered:mixingradle:${requested.version}")
            }
        }
    }
}
rootProject.name = "CubicChunks"
includeBuild("CubicChunksAPI") {
    dependencySubstitution {
        substitute(module("io.github.opencubicchunks:cubicchunks-api")).using(project(":"))
    }
}