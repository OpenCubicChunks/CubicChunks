import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.userdev.tasks.RenameJarInPlace
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.format.DateTimeFormatter

plugins {
    java
    `maven-publish`
    signing
    idea
    id("net.minecraftforge.gradle").version("6.+")
    id("org.spongepowered.mixin").version("0.7-SNAPSHOT")
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("com.github.hierynomus.license").version("0.16.1")
    id("io.github.opencubicchunks.gradle.mcGitVersion")
    id("io.github.opencubicchunks.gradle.mixingen")
}

val licenseYear: String by project
val projectName: String by project
val doRelease: String by project
val theForgeVersion: String by project

group = "io.github.opencubicchunks"

base {
    archivesName.set("CubicChunks")
}

mcGitVersion {
    isSnapshot = true
    setCommitVersion("tags/v0.0", "0.0")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

minecraft {
    mappings("stable", "39-1.12")

    val args = listOf(
            "-Dfml.coreMods.load=io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod", //the core mod class, needed for mixins
            "-Dmixin.env.compatLevel=JAVA_8", //needed to use java 8 when using mixins
            "-Dmixin.debug.verbose=true", //verbose mixin output for easier debugging of mixins
            "-Dmixin.debug.export=true", //export classes from mixin to runDirectory/.mixin.out
            "-Dcubicchunks.debug=true", //various debug options of cubic chunks mod. Adds items that are not normally there!
            "-XX:-OmitStackTraceInFastThrow", //without this sometimes you end up with exception with empty stacktrace
            "-Dmixin.checks.interfaces=true", //check if all interface methods are overriden in mixin
            "-Dfml.noGrab=false", //change to disable Minecraft taking control over mouse
            "-ea", //enable assertions
            "-da:io.netty..." //disable netty assertions because they sometimes fail
    )

    runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            jvmArgs(args)
        }

        create("server") {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            jvmArgs(args)
        }
    }
}

sourceSets {
    create("optifine_dummy")
    // TODO: make this unnecessary, it's an awful hack
    create("api") {
        if (!System.getProperty("idea.sync.active", "false").toBoolean()) {
            java {
                srcDir("CubicChunksAPI/src/main/java")
            }
            resources {
                srcDir("CubicChunksAPI/src/main/resources")
            }
            compileClasspath = sourceSets.main.get().compileClasspath
        }
    }
}

val embed: Configuration by configurations.creating
val coreShadow: Configuration by configurations.creating

configurations {
    implementation {
        extendsFrom(embed)
        extendsFrom(coreShadow)
    }
}

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
    minecraft(group = "net.minecraftforge", name = "forge", version = theForgeVersion)

    embed("com.flowpowered:flow-noise:1.0.1-SNAPSHOT")

    compileOnly(sourceSets["optifine_dummy"].output)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.hamcrest:hamcrest-junit:2.0.0.0")
    testImplementation("it.ozimov:java7-hamcrest-matchers:1.3.0")
    testImplementation("org.mockito:mockito-core:4.2.0")
    testImplementation("org.spongepowered:launchwrappertestsuite:1.0-SNAPSHOT")

    coreShadow("org.spongepowered:mixin:0.8.3-SNAPSHOT") {
        isTransitive = false
    }

    embed("io.github.opencubicchunks:regionlib:0.78.0-SNAPSHOT")
    implementation("io.github.opencubicchunks:cubicchunks-api:1.12.2-0.0-SNAPSHOT")
    if (!System.getProperty("idea.sync.active", "false").toBoolean()) {
        annotationProcessor("org.spongepowered:mixin:0.8.4:processor")
    }
}

idea {
    module.apply {
        inheritOutputDirs = true
    }
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

mixin {
    val forgeMinorVersion = theForgeVersion.split(Regex("-")).getOrNull(1)?.split(Regex("\\."))?.getOrNull(1)
            ?: throw IllegalStateException("Couldn't parse forge version")
    token("MC_FORGE", forgeMinorVersion)
}

sourceSets.main {
    ext["refMap"] = "cubicchunks.mixins.refmap.json"
}

mixinGen {
    filePattern = "cubicchunks.mixins.%s.json"
    defaultRefmap = "cubicchunks.mixins.refmap.json"
    defaultPackagePrefix = "io.github.opencubicchunks.cubicchunks.core.asm.mixin"
    defaultCompatibilityLevel = "JAVA_8"
    defaultMinVersion = "0.7.10"

    config("core") {
        required = true
        conformVisibility = true
        injectorsDefaultRequire = 1
    }
    config("fixes") {
        required = false
        conformVisibility = true
    }
    config("noncritical") {
        required = false
        conformVisibility = true
    }
    config("selectable") {
        required = true
        conformVisibility = true
        injectorsDefaultRequire = 1
        configurationPlugin = "io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksMixinConfig"
    }
}

tasks {
    test {
        systemProperty("lwts.tweaker", "cubicchunks.tweaker.MixinTweakerServer")
        jvmArgs("-Dmixin.debug.verbose=true", //verbose mixin output for easier debugging of mixins
                "-Dmixin.checks.interfaces=true", //check if all interface methods are overriden in mixin
                "-Dmixin.env.remapRefMap=true")
        testLogging {
            showStandardStreams = true
        }
    }

    fun substituteVersion(jar: Jar) {
        val fs = FileSystems.newFileSystem(jar.archiveFile.get().asFile.toPath(), jar.javaClass.classLoader)
        var str = String(Files.readAllBytes(fs.getPath("mcmod.info")), StandardCharsets.UTF_8)
        str = str.replace("%%VERSION%%", project.version.toString())
        Files.write(fs.getPath("mcmod.info"), str.toByteArray(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING)
        fs.close()
    }

    fun configureManifest(manifest: Manifest) {
        manifest.attributes(
                "Specification-Title" to project.name,
                "Specification-Version" to project.version,
                "Specification-Vendor" to "OpenCubicChunks",
                "Implementation-Title" to "${project.group}.${project.name.toLowerCase().replace(' ', '_')}",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "OpenCubicChunks",
                "Implementation-Timestamp" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                "FMLCorePlugin" to "io.github.opencubicchunks.cubicchunks.core.asm.coremod.CubicChunksCoreMod",
                "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                "TweakOrder" to "0",
                "ForceLoadAsMod" to "true",
                "FMLCorePluginContainsFMLMod" to "true" // workaround for mixin double-loading the mod on new forge versions
        )
    }

    fun configureShadowJar(task: ShadowJar, classifier: String) {
        task.configurations = listOf(coreShadow)
        task.exclude("META-INF/MUMFREY*")
        task.from(sourceSets["main"].output)
        task.from(sourceSets["api"].output)
        task.exclude("log4j2.xml")
        task.into("/") {
            from(embed)
        }
        task.archiveClassifier.set(classifier)
        configureManifest(task.manifest)
    }

    afterEvaluate {
        getByName("reobfJar").enabled = false;
    }

    jar {
        from(sourceSets["main"].output)
        from(sourceSets["api"].output)
        exclude("LICENSE.txt", "log4j2.xml")
        configureManifest(manifest)
        archiveClassifier.set("dev")
        doLast {
            substituteVersion(this as Jar)
        }
    }

    compileJava {
        options.isDeprecation = true
        options.compilerArgs.add("-Xlint:unchecked")
    }

    javadoc {
        source(sourceSets.main.get().allJava, sourceSets["api"].allJava)
        (options as StandardJavadocDocletOptions).tags = listOf("reason")
    }

    val deobfSourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].java.srcDirs)
        from(sourceSets["api"].java.srcDirs)
    }

    val devShadowJar by creating(ShadowJar::class) {
        configureShadowJar(this, "dev-all")
        doLast {
            substituteVersion(this as Jar)
        }
    }

    val deobfApiJar by creating(Jar::class) {
        archiveClassifier.set("api-dev")
        from(sourceSets["api"].output)
    }

    val deobfApiSrcJar by creating(Jar::class) {
        archiveClassifier.set("api-sources")
        from(sourceSets["api"].java.srcDirs)
    }

    val javadocApi by creating(Javadoc::class) {
        source = sourceSets["api"].allJava
        doFirst {
            classpath = configurations.compileClasspath.get()
        }
    }

    // gradle complains about using output of javadocApi in javadocJar and javadoc in javadocApiJar when not specifying dependencies on them
    // I don't know why
    val javadocJar by creating(Jar::class) {
        dependsOn(javadoc, javadocApi)
        archiveClassifier.set("javadoc")
        from(javadoc)
    }

    val javadocApiJar by creating(Jar::class) {
        dependsOn(javadocApi, javadoc)
        archiveClassifier.set("api-javadoc")
        from(javadocApi)
    }

    val apiJar by creating(Jar::class) {
        archiveClassifier.set("api")
        from(sourceSets["api"].output)
    }

    shadowJar {
        configureShadowJar(this, "all")
        doLast {
            substituteVersion(this as Jar)
        }
    }

    reobf {
        create("apiJar")
        create("shadowJar")
    }

    apiJar.finalizedBy("reobfApiJar")

    shadowJar {
        finalizedBy("reobfShadowJar")
    }

    build {
        dependsOn(apiJar, deobfApiJar, deobfApiSrcJar, javadocApiJar,
                shadowJar, devShadowJar, javadocJar, deobfSourcesJar)
    }

    afterEvaluate {
        getByName("configureReobfTaskForReobfShadowJar").mustRunAfter("compileJava")
    }

    publish {
        dependsOn(gradle.includedBuild("CubicChunksAPI").task(":publish"))
    }
}

configurations {
    create("mainArchives")
    create("apiArchives")
}

// tasks must be before artifacts, don't change the order
artifacts {
    archives(tasks["shadowJar"])
    add("mainArchives", tasks["devShadowJar"])
    add("mainArchives", tasks["deobfSourcesJar"])
    add("mainArchives", tasks["javadocJar"])

    add("apiArchives", tasks["deobfApiSrcJar"])
    add("apiArchives", tasks["apiJar"])
    add("apiArchives", tasks["javadocApiJar"])
    add("apiArchives", tasks["deobfApiJar"])
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
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"

            setUrl(if (local) localUrl else if (doRelease.toBoolean()) releasesRepoUrl else snapshotsRepoUrl)
            if (!local) {
                credentials {
                    username = user
                    password = pass
                }
            }
        }
    }
    publications {
        create("mod", MavenPublication::class) {
            version = project.ext["mavenProjectVersion"]!!.toString()
            artifactId = "cubicchunks"
            artifact(tasks["shadowJar"]) {
                classifier = ""
            }
            artifact(tasks["devShadowJar"]) {
                classifier = "dev"
            }
            artifact(tasks["deobfSourcesJar"]) {
                classifier = "sources"
            }
            artifact(tasks["javadocJar"]) {
                classifier = "javadoc"
            }
            pom {
                name.set(projectName)
                description.set("Unlimited world height mod for Minecraft")
                packaging = "jar"
                url.set("https://github.com/OpenCubicChunks/CubicChunks")
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
    tasks["publishModPublicationToMavenRepository"].dependsOn("shadowJar", "devShadowJar")
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
    exclude("io/github/opencubicchunks/cubicchunks/core/server/chunkio/async/forge/*") // Taken from forge
    exclude("io/github/opencubicchunks/cubicchunks/core/lighting/phosphor/*") // Taken from Phosphor
    exclude("net/optifine/**/*")
    header = file("HEADER.txt")
    ignoreFailures = false
    strictCheck = true
    mapping(mapOf("java" to "SLASHSTAR_STYLE"))
}