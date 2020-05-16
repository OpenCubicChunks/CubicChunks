package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.CubicChunks;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkPrimer.class)
public class MixinChunkPrimer {

    /*
  This is done to raise worldheight.
   */
    @Final
    @Shadow
    private final ChunkSection[] sections = new ChunkSection[Math.round((float) CubicChunks.worldMAXHeight / 16)];

}
