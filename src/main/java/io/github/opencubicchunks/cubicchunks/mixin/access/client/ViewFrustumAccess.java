package io.github.opencubicchunks.cubicchunks.mixin.access.client;

import io.github.opencubicchunks.cubicchunks.mixin.optifine.client.common.MixinWorldRenderer_Common;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ViewFrustum.class)
public interface ViewFrustumAccess {
    /**
    Allows us to get this method in {@link MixinWorldRenderer_Common}
     */

    @Invoker("getRenderChunk")
    ChunkRenderDispatcher.ChunkRender getRenderChunkAt(BlockPos pos);


}
