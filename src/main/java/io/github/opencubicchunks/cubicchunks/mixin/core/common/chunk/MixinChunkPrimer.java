package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkPrimer.class)
public abstract class MixinChunkPrimer {

    //This is done to raise worldheight. Definitely not permanent.
    @Final @Shadow private final ChunkSection[] sections = new ChunkSection[Math.round((float) CubicChunks.worldMAXHeight / 16)];

}
