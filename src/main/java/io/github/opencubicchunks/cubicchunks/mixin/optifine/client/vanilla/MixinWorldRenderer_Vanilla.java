package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.vanilla;

import io.github.opencubicchunks.cubicchunks.mixin.access.client.ViewFrustumAccess;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_Vanilla {

    @Shadow private int renderDistanceChunks;

    @Shadow private ViewFrustum viewFrustum;

    /**
     * @author Barteks2x
     */
    @javax.annotation.Nullable
    @Overwrite
    private ChunkRenderDispatcher.ChunkRender getRenderChunkOffset(
            BlockPos playerPos, ChunkRenderDispatcher.ChunkRender renderChunkBase, Direction facing) {

        BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);
        if (MathHelper.abs(playerPos.getX() - blockpos.getX()) <= this.renderDistanceChunks * 16
                && MathHelper.abs(playerPos.getY() - blockpos.getY()) <= this.renderDistanceChunks * 16
                && MathHelper.abs(playerPos.getZ() - blockpos.getZ()) <= this.renderDistanceChunks * 16) {
            return ((ViewFrustumAccess) this.viewFrustum).getRenderChunkAt(blockpos);
        }
        return null;
    }
}
