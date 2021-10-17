# Cubic Chunks Minecraft Mod

This MinecraftForge mod extends Minecraft height and depth. The only limitation is size of 32-bit integer.

[![Build Status](https://travis-ci.org/OpenCubicChunks/CubicChunks.svg?branch=MC_1.12)](https://travis-ci.org/OpenCubicChunks/CubicChunks)

For the most up to date information about this mod and its related mods, as well as the newest Downloads, please join us on the Cubic Chunks Discord linked below:  
[Discord server](https://discord.gg/kMfWg9m)

**Cubic Chunks (CC)** - Links:

[Github - Cubic Chunks](https://github.com/OpenCubicChunks/CubicChunks)  
[CurseForge - Main page](https://www.curseforge.com/minecraft/mc-mods/opencubicchunks)  
[CurseForge - Downloads](https://www.curseforge.com/minecraft/mc-mods/opencubicchunks/files/all)

**Cubic World Gen (CWG)** - Links:

[Github - Cubic World Gen](https://github.com/OpenCubicChunks/CubicWorldGen)  
[CurseForge - Main page](https://www.curseforge.com/minecraft/mc-mods/cubicworldgen)  
[CurseForge - Downloads](https://www.curseforge.com/minecraft/mc-mods/cubicworldgen/files/all)

### Cloning the repository

* Please go to our Discord server linked above for the newest info on compilation of this project. 

Note: you need git installed to do the following:
```
git clone --recursive
```
You need a git submodule for the project to compile.
If you don't yet have the submodule but already cloned the repository:
```
git submodule update --init --recursive
```

To get latest version of the submodule:
```
git submodule update --recursive --remote
```

### Compiling the mod
Note: on windows you need to run these commands without `./`

This command:
```
./gradlew build
```
Should be enough to build the mod, but if there are any issues, run `./gradlew setupDecompWorkspace` before `./gradlew build`.
The mod uses information from git repository to generate version number. Make sure you have the git repository before compiling.

### Setting up development environment
Note: on windows you need to run these commands without `./`

![IntelliJ IDEA](intellij-logo.png)

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

For development in IntelliJ IDEA the [MinecraftDev plugin](https://plugins.jetbrains.com/idea/plugin/8327-minecraft-development) is recommended.


#### Other IDEs:

Importing cubic chunks should be the same as any other Forge mod. If the IDE has gradle integration, import the mod as gradle project after setting
 up development environment.
 
To run this mod from your IDE you need to add at least the following JVM option:
```
-Dfml.coreMods.load=cubicchunks.asm.CubicChunksCoreMod
```

If you use a different IDE and know how to setup development environment in that IDEs - submit pull request adding that information to this file.

Some other useful options:

`-Dmixin.debug.verbose=true` - enable mixin verbose output
`-Dmixin.debug.export=true` - export classes after applying mixins to `run/.mixin.out/`, useful for debugging mixins
`-Dcubicchunks.debug=true` - enable cubic chunks debug options
`-XX:-OmitStackTraceInFastThrow` - some parts of cubic chunks code cause fast throw hen they fail, use when you see exception with no stacktrace
`-Dmixin.checks.interfaces=true` - check that mixin classes implement all interface methods
`-Dfml.noGrab=false` - can be useful for debugging client on some systems, disables hiding mouse cursor


