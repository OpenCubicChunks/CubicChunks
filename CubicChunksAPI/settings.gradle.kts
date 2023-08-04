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
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.5.0")
}
rootProject.name = "CubicChunksAPI"