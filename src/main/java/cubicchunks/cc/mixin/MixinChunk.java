package cubicchunks.cc.mixin;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Chunk.class)
public class MixinChunk {
    /*
    This is done to raise worldheight.
     */

    @Final
    @Shadow
    private final ChunkSection[] sections = new ChunkSection[32];
}
