buildscript {
    repositories {
        mavenCentral()
        jcenter()
        maven {
            setUrl("http://files.minecraftforge.net/maven")
        }
        maven {
            setUrl("http://repo.spongepowered.org/maven")
        }
        maven {
            setUrl("https://plugins.gradle.org/m2/")
        }
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:2.3-SNAPSHOT")
    }
}

val theForgeVersion: String by project
val theMappingsVersion: String by project

allprojects {
    apply {
        plugin<BasePlugin>()
        plugin<JavaPlugin>()
        plugin<IdeaPlugin>()
        plugin("io.github.opencubicchunks.gradle.fg2fixed")
        plugin("io.github.opencubicchunks.gradle.remapper")
        plugin("io.github.opencubicchunks.gradle.mcGitVersion")
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
    mcGitVersion {
        isSnapshot = true
    }
    minecraft {
        version = theForgeVersion
        runDir = "run"
        mappings = theMappingsVersion

        replace("@@VERSION@@", project.version.toString())
        replace("public static final boolean IS_DEV = true;", "public static final boolean IS_DEV = false;")
        replaceIn("io/github/opencubicchunks/cubicchunks/core/CubicChunks.java")

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

        clientJvmArgs.addAll(args)
        serverJvmArgs.addAll(args)
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
}

dependencies {
    implementation(project(":CubicChunks"))
}