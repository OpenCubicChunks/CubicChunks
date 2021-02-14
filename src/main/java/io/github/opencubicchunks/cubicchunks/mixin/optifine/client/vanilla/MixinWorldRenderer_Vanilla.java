package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.vanilla;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.client.IVerticalViewDistance;
import io.github.opencubicchunks.cubicchunks.mixin.access.client.ViewFrustumAccess;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class MixinWorldRenderer_Vanilla {

    @Shadow private int lastViewDistance;

    @Shadow private ViewArea viewArea;

    @Shadow private ClientLevel level;

    @Shadow @Final private Minecraft minecraft;

    @Shadow public abstract void allChanged();

    private int lastVerticalViewDistance = -1;


    /**
     * @author AidanLovelace
     * @reason Make sure we load the renderers again if the vertical view distance changed but not if the normal render distance changed because then they'll be loaded again anyways.
     */
    @Inject(method = "setupRender", at = @At("HEAD"))
    private void setupVerticalViewDistance(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator, CallbackInfo ci) {
        if (this.minecraft.options.renderDistance != this.lastViewDistance) return;
        if (this.lastVerticalViewDistance != ((IVerticalViewDistance) this.minecraft.options).getVerticalViewDistance()) {
            this.allChanged();
        }
    }

    @Inject(method = "allChanged", at = @At("HEAD"))
    private void setLastVerticalViewDistance(CallbackInfo ci) {
        this.lastVerticalViewDistance = ((IVerticalViewDistance) this.minecraft.options).getVerticalViewDistance();
    }

    /**
     * @author Barteks2x
     * @reason Vanilla doesn't use y pos, and constrains between 0 and 256
     */
    @Nullable
    @Inject(method = "getRelativeFrom", at = @At("HEAD"), cancellable = true)
    private void getRelativeFrom(BlockPos playerPos, ChunkRenderDispatcher.RenderChunk renderChunkBase, Direction facing,
                                 CallbackInfoReturnable<ChunkRenderDispatcher.RenderChunk> cir) {

        if (!((CubicLevelHeightAccessor) this.level).isCubic()) {
            return;
        }

        BlockPos blockpos = renderChunkBase.getRelativeOrigin(facing);
        if (Mth.abs(playerPos.getX() - blockpos.getX()) <= this.lastViewDistance * 16
            && Mth.abs(playerPos.getY() - blockpos.getY()) <= this.lastViewDistance * 16
            && Mth.abs(playerPos.getZ() - blockpos.getZ()) <= this.lastViewDistance * 16) {
            cir.setReturnValue(((ViewFrustumAccess) this.viewArea).invokeGetRenderChunkAt(blockpos));
        }
    }
}