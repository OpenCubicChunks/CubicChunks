# Cubic Chunks Minecraft Mod

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/CubicChunks-dev/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


[![Build Status](https://travis-ci.org/OpenCubicChunks/CubicChunks.svg?branch=master)](https://travis-ci.org/OpenCubicChunks/CubicChunks)

This MinecraftForge mod extends Minecraft height and depth. The only limitation is size of 32-bit integer.

### Cloning the repository
Note: you need git installed to do that
```
git clone --recursive https://github.com/Barteks2x/CubicChunks.git
```
If you already cloned it without `--recursive` option before reading this, run:
```
git submodule update --init --recursive
```

### Compiling the mod
Note: on windows you need to run these commands without `./`

This command:
```
./gradlew build
```
~~should be enough to compile the mod.~~ Because of [a bug in ForgeGradle](https://github.com/MinecraftForge/ForgeGradle/issues/410) running one of: `./gradlew setupCiWorkspace`, `./gradlew setupDevWorkspace`, `./gradlew setupDecompWorkspace` is required before running `./gradlew build` for the first time, and after some dependency updates. (`setupCiWorkspace` does the minimum required to compile the mod)

The mod uses information from git repository to generate version number, it won't compile without full git repository. It also won't compile if regionlib submodule doesn't exist.

### Setting up development environment
Note: on windows you need to run these commands without `./`

#### intellij IDEA
Run:
```
./gradlew setupDecompWorkspace
```
then import it as gradle project into IDEA (if you already have something open, use File->new->project from existing sources)
Then run:
```
./gradlew genIntellijRuns
```
To be able to run the mod from within IDE. Then edit the generated run configurations and set `use classpath of module` to `CubicChunkc_main`
Then refresh gradle project in IDEA.

For development in Intellij IDEA the [MinecraftDev plugin](https://plugins.jetbrains.com/idea/plugin/8327-minecraft-development) is recommended.


#### Other IDEs:

I don't use any other IDE so I don't know how to do that, but you should be able to find instructions for importing forge mod into your IDE.

To run this mod from your IDE you need to add at least the following JVM options:
```
-Dfml.coreMods.load=cubicchunks.asm.CubicChunksCoreMod -Dmixin.env.compatLevel=JAVA_8
```

If you know how to setup development environment in other IDEs - submit pull request adding that information to this file.

Some other useful options:

`-Dmixin.debug.verbose=true` - enable mixin verbose output
`-Dmixin.debug.export=true` - export classes after applying mixins to `run/.mixin.out/`, useful for debugging mixins
`-Dcubicchunks.debug=true` - enable cubic chunks debug options
`-XX:-OmitStackTraceInFastThrow` - some parts of cubic chunks code cause fast throw hen they fail, use when you see exception with no stacktrace
`-Dmixin.checks.interfaces=true` - check that mixin classes implement all interface methods
`-Dfml.noGrab=false` - can be useful for debugging client on some systems, disables hiding mouse cursor


