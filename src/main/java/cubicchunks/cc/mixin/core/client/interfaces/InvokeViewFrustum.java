package cubicchunks.cc.mixin.core.client.interfaces;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewFrustum.class)
public interface InvokeViewFrustum {
    /**
    Allows us to get this method in {@link cubicchunks.cc.mixin.core.client.MixinWorldRenderer}
     */

    @Invoker("getRenderChunk")
    ChunkRenderDispatcher.ChunkRender getRenderChunkAt(BlockPos pos);


}
