import net.minecraftforge.gradle.tasks.DeobfuscateJar
import nl.javadude.gradle.plugins.license.LicensePlugin
import org.gradle.api.internal.HasConvention
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.extra

import kotlin.apply

// Gradle repositories and dependencies
buildscript {
    repositories {
        mavenCentral()
        maven {
            setUrl("https://repo.spongepowered.org/maven")
        }
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
        maven {
            setUrl("https://maven.minecraftforge.net/")
        }
    }
    dependencies {
        classpath("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.14.0")
        classpath("net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT")
    }
}

plugins {
    base
    java
    idea
    eclipse
    `maven-publish`
    maven
    signing
    id("io.github.opencubicchunks.gradle.fg2fixed")
    id("io.github.opencubicchunks.gradle.remapper")
    id("io.github.opencubicchunks.gradle.mcGitVersion")
}

apply {
    plugin<LicensePlugin>()
}

mcGitVersion {
    isSnapshot = true
    setCommitVersion("tags/v0.0", "0.0")
}

// tasks
val build by tasks
val jar: Jar by tasks
val sourceJar: Jar by tasks
val javadoc: Javadoc by tasks
val test: Test by tasks
val processResources: ProcessResources by tasks
val deobfMcSRG: DeobfuscateJar by tasks
val deobfMcMCP: DeobfuscateJar by tasks
val compileJava : JavaCompile by tasks

defaultTasks = listOf("licenseFormat", "build")

//it can't be named forgeVersion because ForgeExtension has property named forgeVersion
val theForgeVersion: String by project
val theMappingsVersion: String by project

val licenseYear: String by project
val projectName: String by project

val versionSuffix: String by project
val versionMinorFreeze: String by project
val release: String by project

val sourceSets = the<JavaPluginConvention>().sourceSets
val mainSourceSet = sourceSets["main"]!!

group = "io.github.opencubicchunks"
(mainSourceSet as ExtensionAware).extra["refMap"] = "cubicchunks.mixins.refmap.json"

sourceSets {
    create("optifine_dummy")
}

val compile by configurations
val testCompile by configurations

dependencies {
    testCompile("junit:junit:4.11")
    testCompile("org.hamcrest:hamcrest-junit:2.0.0.0")
    testCompile("org.mockito:mockito-core:2.1.0-RC.2")
}

idea {
    module.apply {
        inheritOutputDirs = true
    }
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

base {
    archivesBaseName = "CubicChunks"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

minecraft {
    version = theForgeVersion
    runDir = "run"
    mappings = theMappingsVersion
}

license {
    val ext = (this as HasConvention).convention.extraProperties
    ext["project"] = projectName
    ext["year"] = licenseYear
    exclude("**/*.info")
    exclude("**/package-info.java")
    exclude("**/*.json")
    exclude("**/*.xml")
    exclude("assets/*")
    header = file("HEADER.txt")
    ignoreFailures = false
    strictCheck = true
    mapping(mapOf("java" to "SLASHSTAR_STYLE"))
}

compileJava.apply {
    options.isDeprecation = true
}

val deobfJar by tasks.creating(Jar::class) {
    classifier = "api-dev"
    from(sourceSets["main"].output)
}

val deobfSrcJar by tasks.creating(Jar::class) {
    classifier = "api-sources"
    from(sourceSets["main"].java.srcDirs)
}

val javadocJar by tasks.creating(Jar::class) {
    classifier = "api-javadoc"
    from(tasks["javadoc"])
}

sourceJar.apply {
    classifier = "api-sources-srg"
}

jar.apply {
    classifier = "api"
}

artifacts {
    withGroovyBuilder {
        "archives"(jar, deobfSrcJar, javadocJar, sourceJar, deobfSrcJar, deobfJar)
    }
}

publishing {
    repositories {
        maven {
            val user = (project.properties["sonatypeUsername"] ?: System.getenv("sonatypeUsername")) as String?
            val pass = (project.properties["sonatypePassword"] ?: System.getenv("sonatypePassword")) as String?
            val local = user == null || pass == null
            if (local) {
                logger.warn("Username or password not set, publishing to local repository in build/mvnrepo/")
            }
            val localUrl = "$buildDir/mvnrepo"
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl =  "https://oss.sonatype.org/content/repositories/snapshots"

            setUrl(if (local) localUrl else if (release.toBoolean()) releasesRepoUrl else snapshotsRepoUrl)
            if (!local) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }
    }
    (publications) {
        "api"(MavenPublication::class) {
            version = ext["mavenProjectVersion"]!!.toString()
            artifactId = "cubicchunks-api"
            from(components["java"])
            artifacts.clear()
            artifact(deobfSrcJar) {
                classifier = "sources"
            }
            artifact(jar)
            artifact(javadocJar) {
                classifier = "javadoc"
            }
            pom {
                name.set("Cubic Chunks API")
                description.set("API for the CubicChunks mod for Minecraft")
                packaging = "jar"
                url.set("https://github.com/OpenCubicChunks/CubicChunks")
                description.set("API for CubicChunks mod for Minecraft")
                scm {
                    connection.set("scm:git:git://github.com/OpenCubicChunks/CubicChunks.git")
                    developerConnection.set("scm:git:ssh://git@github.com:OpenCubicChunks/CubicChunks.git")
                    url.set("https://github.com/OpenCubicChunks/CubicChunks")
                }

                licenses {
                    license {
                        name.set("The MIT License")
                        url.set("http://www.tldrlegal.com/license/mit-license")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("Barteks2x")
                        name.set("Barteks2x")
                    }
                    // TODO: add more developers
                }

                issueManagement {
                    system.set("github")
                    url.set("https://github.com/OpenCubicChunks/CubicChunks/issues")
                }
            }
        }
    }
}

signing {
    isRequired = false
    // isRequired = gradle.taskGraph.hasTask("uploadArchives")
    sign(configurations.archives)
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        setUrl("https://oss.sonatype.org/content/repositories/public/")
    }
    maven {
        setUrl("https://maven.minecraftforge.net")
    }
    maven {
        setUrl("https://repo.spongepowered.org/maven")
    }
}
repositories.removeIf { it is MavenArtifactRepository && it.url.toString().contains("files.minecraftforge") }

jar.apply {
    from(sourceSets["main"].output)
    exclude("LICENSE.txt")
}

fun extractForgeMinorVersion(): String {
    // version format: MC_VERSION-MAJOR.MINOR.?.BUILD
    return theForgeVersion.split(Regex("-")).getOrNull(1)?.split(Regex("\\."))?.getOrNull(1) ?:
    throw RuntimeException("Invalid forge version format: $theForgeVersion")
}
