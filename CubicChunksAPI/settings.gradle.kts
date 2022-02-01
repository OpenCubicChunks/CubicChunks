pluginManagement {
    repositories {
        maven {
            setUrl("https://maven.minecraftforge.net/")
        }
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "net.minecraftforge.gradle") {
                useModule("${requested.id}:ForgeGradle:${requested.version}")
            }
        }
    }
}
rootProject.name = "CubicChunksAPI"