import net.minecraftforge.gradle.common.util.RunConfig
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.operation.DescribeOp
import org.gradle.api.internal.HasConvention
import java.text.SimpleDateFormat
import java.util.*

buildscript {
    repositories {
        maven { setUrl("https://files.minecraftforge.net/maven") }
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:3.+")
    }
}
plugins {
    idea
    eclipse
    `maven-publish`
    signing
    id("org.ajoberstar.grgit").version("3.1.1")
    id("com.github.johnrengelman.shadow").version("4.0.2")
    id("com.github.hierynomus.license").version("0.15.0")
}
apply(plugin = "net.minecraftforge.gradle")

val modid: String by project
val forgeVersion: String by project
val mappingsVersion: String by project

val versionSuffix: String by project
val versionMinorFreeze: String by project
val release: String by project

version = getModVersion()
group = "io.github.opencubicchunks"

base {
    archivesBaseName = modid
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

idea {
    module.apply {
        inheritOutputDirs = true
    }
    module.isDownloadJavadoc = true
    module.isDownloadSources = true
}

minecraft {
    mappings("snapshot", mappingsVersion)

    fun setupConfig(conf: RunConfig) {
        conf.apply {
            property("forge.logging.markers", "SCAN,REGISTRIES,REGISTRYDUMP")
            property("forge.logging.console.level", "debug")
            property("mixin.temphack.configs", "cubicchunks.mixins.core.json")
            property("mixin.debug.verbose", "true")
            property("mixin.debug.export", "true")
            property("mixin.checks.interfaces", "true")
            property("cubicchunks.debug", "true")
            jvmArgs(listOf(
                    "-XX:-OmitStackTraceInFastThrow", //without this sometimes you end up with exception with empty stacktrace
                    "-ea", //enable assertions
                    "-da:io.netty..." //disable netty assertions because they sometimes fail
            ))

            mods.create("examplemod") {
                source(java.sourceSets["main"])
            }
        }
    }

    runs.create("client") {
        workingDirectory(project.file("run"))
        setupConfig(this)
    }
    runs.create("server") {
        workingDirectory(project.file("run"))
        setupConfig(this)
    }
}

license {
    val ext = (this as HasConvention).convention.extraProperties
    ext["project"] = "CubicChunks"
    ext["year"] = "2019"
    exclude("**/*.info")
    exclude("**/package-info.java")
    exclude("**/*.json")
    exclude("**/*.xml")
    exclude("assets/*")
    exclude("io/github/opencubicchunks/cubicchunks/core/server/chunkio/async/forge/*") // Taken from forge
    exclude("net/optifine/**/*")
    header = file("HEADER.txt")
    ignoreFailures = false
    strictCheck = true
    mapping(mapOf("java" to "SLASHSTAR_STYLE"))
}

dependencies {
    minecraft("net.minecraftforge:forge:$forgeVersion")

    val mixinFile = project.file("../../../Mixin/build/libs/mixin-0.7.11-SNAPSHOT.jar")
    if (mixinFile.exists()) {
        compileOnly(files(mixinFile))
    } else {
        compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
            isTransitive = false
        }
    }

    implementation("io.github.opencubicchunks:regionlib:0.55.0-SNAPSHOT")
}

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    maven {
        setUrl("https://oss.sonatype.org/content/repositories/public/")
    }
    maven {
        setUrl("http://repo.spongepowered.org/maven")
    }
}

tasks.getByName<Jar>("jar") {
    manifest {
        attributes(mapOf(
                "Specification-Title" to modid,
                "Specification-Vendor" to "OpenCubicChunks",
                "Specification-Version" to "1",
                "Implementation-Title" to modid,
                "Implementation-Version" to version,
                "Implementation-Vendor" to "OpenCubicChunks",
                "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date()))
        )
    }
}

tasks.getByName<JavaCompile>("compileJava") {
    options.compilerArgs.addAll(listOf(
            "-AreobfSrgFile=${project.file("dummyMappingsFile.srg")}",
            "-AdefaultObfuscationEnv=searge"
    ))
}


// Example configuration to allow publishing using the maven-publish task
// we define a custom artifact that is sourced from the reobfJar output task
// and then declare that to be published
// Note you'll need to add a repository here
val reobfFile = file("$buildDir/reobfJar/output.jar")
val reobfArtifact = artifacts.add("default", reobfFile) {
    type = "jar"
    builtBy("reobfJar")
}

publishing {
    publications.create("mavenJava", MavenPublication::class.java) {
        artifact(reobfArtifact)
    }
    repositories {
        maven {
            setUrl("file:///${project.projectDir}/mcmodsrepo")
        }
    }
}
// Version handling

fun getMcVersion(): String {
    return forgeVersion.split("-")[0]
}

//returns version string according to this: http://mcforge.readthedocs.org/en/latest/conventions/versioning/
//format: MCVERSION-MAJORMOD.MAJORAPI.MINOR.PATCH(-final/rcX/betaX)
//rcX and betaX are not implemented yet
fun getModVersion(): String {
    return getModVersion(false)
}

//returns version string similar to that here: http://mcforge.readthedocs.org/en/latest/conventions/versioning/
//but without minor and patch versions, and with -SNAPSHOT if not doing maven release
//format: MCVERSION-MAJORMOD.MAJORAPI(-final/rcX/betaX)(-SNAPSHOT)
//rcX and betaX are not implemented yet
fun getModVersionMaven(): String {
    return getModVersion(true)
}

fun getModVersion(maven: Boolean): String {
    var version = "${getMcVersion()}-9999.9999.9999-NOVERSION"
    try {
        val git = Grgit.open()
        val describe = DescribeOp(git.repository).call()
        val branch = getGitBranch(git)
        val snapshotSuffix = if (release.toBoolean()) "" else "-SNAPSHOT"
        version = getModVersion(describe, branch, maven) + snapshotSuffix;
    } catch (ex: Exception) {
        logger.error("Unknown error when accessing git repository! Are you sure the git repository exists?", ex)
    }
    println("Project version string: $version")
    return version
}

fun getGitBranch(git: Grgit): String {
    var branch: String = git.branch.current().name
    if (branch == "HEAD") {
        branch = when {
            System.getenv("TRAVIS_BRANCH")?.isEmpty() == false -> // travis
                System.getenv("TRAVIS_BRANCH")
            System.getenv("GIT_BRANCH")?.isEmpty() == false -> // jenkins
                System.getenv("GIT_BRANCH")
            System.getenv("BRANCH_NAME")?.isEmpty() == false -> // ??? another jenkins alternative?
                System.getenv("BRANCH_NAME")
            else -> throw RuntimeException("Found HEAD branch! This is most likely caused by detached head state! Will assume unknown version!")
        }
    }

    if (branch.startsWith("origin/")) {
        branch = branch.substring("origin/".length)
    }
    return branch
}

fun getModVersion(describe: String, branch: String, mvn: Boolean): String {
    if (branch.startsWith("MC_")) {
        val branchMcVersion = branch.substring("MC_".length)
        if (branchMcVersion != getMcVersion()) {
            logger.warn("Branch version different than project MC version! MC version: " +
                    getMcVersion() + ", branch: " + branch + ", branch version: " + branchMcVersion)
        }
    }

    //branches "master" and "MC_something" are not appended to version sreing, everything else is
    //only builds from "master" and "MC_version" branches will actually use the correct versioning
    //but it allows to distinguish between builds from different branches even if version number is the same
    val branchSuffix = if (branch == "master" || branch.startsWith("MC_")) "" else ("-" + branch.replace("[^a-zA-Z0-9.-]", "_"))

    val baseVersionRegex = "v[0-9]+\\.[0-9]+"
    val unknownVersion = String.format("%s-UNKNOWN_VERSION%s%s", getMcVersion(), versionSuffix, branchSuffix)
    if (!describe.contains('-')) {
        //is it the "vX.Y" format?
        if (describe.matches(Regex(baseVersionRegex))) {
            return if (mvn) String.format("%s-%s", getMcVersion(), describe)
            else String.format("%s-%s.0.0%s%s", getMcVersion(), describe, versionSuffix, branchSuffix)
        }
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    //Describe format: vX.Y-build-hash
    val parts = describe.split("-")
    if (!parts[0].matches(Regex(baseVersionRegex))) {
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    if (!parts[1].matches(Regex("[0-9]+"))) {
        logger.error("Git describe information: \"$describe\" in unknown/incorrect format")
        return unknownVersion
    }
    val mcVersion = getMcVersion()
    val modAndApiVersion = parts[0].substring(1)
    //next we have commit-since-tag
    val commitSinceTag = Integer.parseInt(parts[1])

    val minorFreeze = if (versionMinorFreeze.isEmpty()) -1 else Integer.parseInt(versionMinorFreeze)

    val minor = if (minorFreeze < 0) commitSinceTag else minorFreeze
    val patch = if (minorFreeze < 0) 0 else (commitSinceTag - minorFreeze)

    return if (mvn) String.format("%s-%s%s", mcVersion, modAndApiVersion, versionSuffix)
    else String.format("%s-%s.%d.%d%s%s", mcVersion, modAndApiVersion, minor, patch, versionSuffix, branchSuffix)
}