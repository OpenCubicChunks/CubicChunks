# Cubic Chunks Minecraft Mod

## WARNING: this is early 1.14 development branch. Everything here is likely to change, the commit history may change too.

gi[Discord server](https://discord.gg/kMfWg9m)

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

If you have some issues, try to make sure gradle daemon isn't running (especially if importing in IDE)
### Setting up development environment
Note: on windows you need to run these commands without `./`

#### intellij IDEA
Iimport it as gradle project into IDEA (if you already have something open, use File->new->project from existing sources)
Then run:
```
./gradlew genIntellijRuns
```
To be able to run the mod from within IDE. Then edit the generated run configurations and set `use classpath of module` to `CubicChunks_main`
Then refresh gradle project in IDEA (make ssure gradle daemon isn't running before you do that).

For development in Intellij IDEA the [MinecraftDev plugin](https://plugins.jetbrains.com/idea/plugin/8327-minecraft-development) is recommended.
