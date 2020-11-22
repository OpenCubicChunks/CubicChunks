package io.github.opencubicchunks.cubicchunks.mixin.asm.common;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
    ChunkMap.DistanceManager.class,
    ChunkMap.class,
    ChunkHolder.class
})
public class MixinAsmTarget {
    // intentionally empty
}