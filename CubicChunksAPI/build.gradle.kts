import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

plugins {
    java
    `maven-publish`
    signing
    idea
    id("net.minecraftforge.gradle").version("6.0.18")
    id("wtf.gofancy.fancygradle").version("1.1.3-0")
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

fancyGradle {
    patches {
        resources
        coremods
        codeChickenLib
        asm
        mergetool
    }
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
                    "Implementation-Title" to "${project.group}.${project.name.lowercase(Locale.ROOT).replace(' ', '_')}",
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
                    "Implementation-Title" to "${project.group}.${project.name.lowercase(Locale.ROOT).replace(' ', '_')}",
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
        fun configureArtifacts(publication: MavenPublication) {
            publication.artifact(tasks["deobfSrcJar"]) {
                classifier = "sources"
            }
            publication.artifact(tasks["allJar"]) {
                classifier = ""
            }
            publication.artifact(tasks["javadocJar"]) {
                classifier = "javadoc"
            }
            publication.artifact(tasks.jar) {
                classifier = "dev"
            }
        }

        fun configurePom(publication: MavenPublication) {
            publication.pom {
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

        create<MavenPublication>("mavenJava") {
            version = project.ext["mavenProjectVersion"]!!.toString()
            artifactId = "cubicchunks-api"

            configureArtifacts(this)
            configurePom(this)
        }

        //same as "mavenJava", but using the full project version from mcGitVersion instead of mavenProjectVersion.
        create<MavenPublication>("versionedMavenJava") {
            version = project.version.toString()
            artifactId = "cubicchunks-api"

            configureArtifacts(this)
            configurePom(this)
        }
    }
    repositories {
        maven {
            name = "central"

            val local = properties["centralAuthHeaderName"] == null
            if (local) {
                logger.warn("Username or password not set, publishing to local repository in build/mvnrepo/")
            }
            val localUrl = "$buildDir/mvnrepo"
            val releasesRepoUrl = "https://central.sonatype.com/api/v1/publisher/deployments/download/"
            val snapshotsRepoUrl = "https://central.sonatype.com/api/v1/publisher/deployments/download/"

            setUrl(if (local) localUrl else if (doRelease.toBoolean()) releasesRepoUrl else snapshotsRepoUrl)
            if (!local) {
                credentials(HttpHeaderCredentials::class)
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }

        //only register maven.daporkchop.net repository if these environment variables are set
        val daporkchopMavenUsername = (project.properties["daporkchopMavenUsername"] ?: System.getenv("daporkchopMavenUsername")) as String?
        val daporkchopMavenPassword = (project.properties["daporkchopMavenPassword"] ?: System.getenv("daporkchopMavenPassword")) as String?
        if (daporkchopMavenUsername != null && daporkchopMavenPassword != null) {
            maven {
                name = "DaPorkchop_"

                val releasesRepoUrl = "https://maven.daporkchop.net/release/"
                val snapshotsRepoUrl = "https://maven.daporkchop.net/snapshot/"

                setUrl(if (doRelease.toBoolean()) releasesRepoUrl else snapshotsRepoUrl)
                credentials {
                    username = daporkchopMavenUsername
                    password = daporkchopMavenPassword
                }
            }
        }
    }

    //all publish tasks should depend on all of the tasks which generate the artifacts being published
    tasks.withType<AbstractPublishToMaven>().configureEach {
        dependsOn("deobfSrcJar", "allJar", "javadocJar", "jar")
    }

    //this is kinda gross, but is apparently the recommended way to conditionally publish specific publications to specific repositories:
    //  see https://docs.gradle.org/current/userguide/publishing_customization.html#sec:publishing_maven:conditional_publishing
    tasks.withType<PublishToMavenRepository>().configureEach {
        val predicate = provider {
            (publication == publications["mavenJava"] && repository == repositories.findByName("Sonatype")) ||
            (publication == publications["versionedMavenJava"] && repository == repositories.findByName("DaPorkchop_"))
        }
        onlyIf("publishing API to Sonatype repository, and versioned API to DaPorkchop_ repository") {
            predicate.get()
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