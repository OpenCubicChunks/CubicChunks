#Cubic Chunks Minecraft Mod

[![Gitter](https://badges.gitter.im/Join Chat.svg)](https://gitter.im/CubicChunks-dev/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Build Status](https://travis-ci.org/Barteks2x/CubicChunks.svg?branch=master)](https://travis-ci.org/Barteks2x/CubicChunks)

This MinecraftForge mod extends Minecraft height and depth to 8388608 in both directions (this height is limitation of save format).

###Running in dev environment

To run this mod from your IDE you need to add at least the following JVM options:
```
-Dfml.coreMods.load=cubicchunks.asm.CoreModLoadingPlugin -Dmixin.env.compatLevel=JAVA_8
```

Some other useful options:

`-Dmixin.debug.verbose=true` - enable mixin verbose output

`-Dmixin.debug.export=true` - export classes after applying mixins to `run/.mixin.out/`, if you add this director as library - it allows you to set breakpoints in mixins and see decompiled code after applying mixins in IDE

`-Dcubicchunks.debug=true` - enable cubic chunks debug options

`-XX:-OmitStackTraceInFastThrow` - some parts of cubic chunks code cause fast throw hen they fail, use when you see exception with no stacktrace
