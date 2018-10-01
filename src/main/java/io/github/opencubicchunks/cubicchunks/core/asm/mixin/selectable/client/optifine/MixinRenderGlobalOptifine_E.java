package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IMinMaxHeight;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobalOptifine_E {
    @Nullable private BlockPos position;

    @Shadow private int renderDistanceChunks;

    @Shadow private ViewFrustum viewFrustum;

    @Shadow private WorldClient world;

    /**
     * Optifine-specific version of the entity render fix. Versions 1.12.2_HD_U_C7_pre and up
     *
     * This method sets position for MixinRenderGlobal#getRenderChunkYPos to use
     */
    @SuppressWarnings("UnresolvedMixinReference")
    @Group(name = "renderEntitiesFix")
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;"),
            locals = LocalCapture.CAPTURE_FAILHARD)
    public void onGetPositionOptifine_New(Entity renderViewEntity, ICamera camera, float partialTicks,
            CallbackInfo ci, int pass, double d0, double d1, double d2,
            Entity entity, double d3, double d4, double d5,
            List list, boolean forgeEntityPass, boolean forgeTileEntityPass, boolean isShaders, boolean oldFancyGraphics, List list1, List list2,
            BlockPos.PooledMutableBlockPos pos, Iterator var22, RenderGlobal.ContainerLocalRenderInformation info) {
        ICubicWorld world = (ICubicWorld) info.renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = info.renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    @ModifyConstant(method = "setupTerrain", constant = @Constant(intValue = 256))
    public int getMaxWorldHeight(int _256) {
        return ((IMinMaxHeight) world).getMaxHeight();
    }
}
