package cubicchunks.cc.mixin;

import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewFrustum.class)
public interface IMixinViewFrustum {
    @Invoker("getRenderChunk")
    ChunkRenderDispatcher.ChunkRender getRenderChunkAt(BlockPos pos);
}
