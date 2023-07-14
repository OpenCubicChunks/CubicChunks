import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    java
    `maven-publish`
    signing
    idea
    id("net.minecraftforge.gradle").version("6.+")
    id("io.github.opencubicchunks.gradle.mcGitVersion")
    id("com.github.hierynomus.license").version("0.16.1")
}

val licenseYear: String by project
val projectName: String by project
val doRelease: String by project

group = "io.github.opencubicchunks"

base {
    archivesName.set("CubicChunksAPI")
}

mcGitVersion {
    isSnapshot = true
    setCommitVersion("tags/v0.0", "0.0")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    maven {
        setUrl("https://oss.sonatype.org/content/repositories/public/")
    }
    maven {
        setUrl("https://repo.spongepowered.org/maven")
    }
}

dependencies {
    minecraft(group = "net.minecraftforge", name = "forge", version = "1.12.2-14.23.5.2860")
}

minecraft {
    mappings("stable", "39-1.12")
}

idea {
    module.apply {
        inheritOutputDirs = true
    }
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

tasks {
    jar {
        from(sourceSets.main.get().output)
        exclude("LICENSE.txt")
        manifest {
            attributes(
                    "Specification-Title" to project.name,
                    "Specification-Version" to project.version,
                    "Specification-Vendor" to "OpenCubicChunks",
                    "Implementation-Title" to "${project.group}.${project.name.toLowerCase().replace(' ', '_')}",
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "OpenCubicChunks",
                    "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )
        }
    }

    afterEvaluate {
        getByName("reobfJar").enabled = false;
    }

    compileJava {
        options.isDeprecation = true
    }

    val allJar by creating(Jar::class) {
        archiveClassifier.set("all")
        from(sourceSets["main"].output)
        exclude("LICENSE.txt")
        manifest {
            attributes(
                    "Specification-Title" to project.name,
                    "Specification-Version" to project.version,
                    "Specification-Vendor" to "OpenCubicChunks",
                    "Implementation-Title" to "${project.group}.${project.name.toLowerCase().replace(' ', '_')}",
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to "OpenCubicChunks",
                    "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now())
            )
        }
    }

    reobf {
        create("allJar")
    }

    publish {
        dependsOn("reobfAllJar")
    }
    allJar.finalizedBy("reobfAllJar")

    val deobfSrcJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().java.srcDirs)
    }

    val javadocJar by creating(Jar::class) {
        archiveClassifier.set("javadoc")
        from(javadoc)
    }
}

artifacts {
    archives(tasks.jar)
    archives(tasks["deobfSrcJar"])
    archives(tasks["javadocJar"])
    archives(tasks["deobfSrcJar"])
    archives(tasks["allJar"])
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            version = project.ext["mavenProjectVersion"]!!.toString()
            artifactId = "cubicchunks-api"
            artifact(tasks["deobfSrcJar"]) {
                classifier = "sources"
            }
            artifact(tasks["allJar"]) {
                classifier = ""
            }
            artifact(tasks["javadocJar"]) {
                classifier = "javadoc"
            }
            artifact(tasks.jar) {
                classifier = "dev"
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

            setUrl(if (local) localUrl else if (doRelease.toBoolean()) releasesRepoUrl else snapshotsRepoUrl)
            if (!local) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }
    }
}

signing {
    isRequired = false
    // isRequired = gradle.taskGraph.hasTask("uploadArchives")
    sign(configurations.archives.get())
}

license {
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