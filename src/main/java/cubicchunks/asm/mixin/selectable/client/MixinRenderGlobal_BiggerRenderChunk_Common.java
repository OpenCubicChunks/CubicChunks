/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.asm.mixin.selectable.client;

import static cubicchunks.client.RenderConstants.RENDER_CHUNK_MAX_POS_OFFSET;
import static cubicchunks.client.RenderConstants.RENDER_CHUNK_SIZE;
import static cubicchunks.client.RenderConstants.RENDER_CHUNK_SIZE_BIT;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cubicchunks.world.ICubicWorldClient;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.ChunkRenderContainer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderGlobal.ContainerLocalRenderInformation;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * Fixes renderEntities crashing when rendering cubes that are not at existing array index in chunk.getEntityLists(), <p> Allows to render cubes outside of 0..256 height range.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal_BiggerRenderChunk_Common {

    @Nullable private BlockPos position;
    @Shadow private int renderDistanceChunks;
    @Shadow private ViewFrustum viewFrustum;
    @Shadow private WorldClient world;
    @Shadow private int renderEntitiesStartupCounter;
    @Shadow @Final private RenderManager renderManager;
    @Shadow private int countEntitiesTotal;
    @Shadow private int countEntitiesRendered;
    @Shadow private int countEntitiesHidden;

    @Shadow
    private boolean isOutlineActive(Entity entityIn, Entity viewer, ICamera camera) {
        throw new AbstractMethodError();
    };

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", remap = false))
    public boolean onIteratingThruRenderInfos(Iterator<RenderGlobal.ContainerLocalRenderInformation> renderInfosIterator, Entity renderViewEntity,
            ICamera camera, float partialTicks) {
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        if (cworld.isCubicWorld())
            return false;
        return renderInfosIterator.hasNext();
    }

    /**
     * Overwrite getRenderChunk(For)Offset to support extended height.
     *
     * @author Barteks2x
     * @reason Remove hardcoded height checks, it's a simple method and doing it differently would be problematic and confusing (Inject with local capture into BlockPos.getX() and redirect of BlockPos.getY())
     */
    @Nullable
    @Overwrite
    private RenderChunk getRenderChunkOffset(BlockPos playerPos, RenderChunk renderChunkBase, EnumFacing facing) {
        BlockPos blockpos = renderChunkBase.getBlockPosOffset16(facing);
        return MathHelper.abs(playerPos.getX() - blockpos.getX()) > this.renderDistanceChunks * RENDER_CHUNK_SIZE ? null
                : MathHelper.abs(playerPos.getY() - blockpos.getY()) > this.renderDistanceChunks * RENDER_CHUNK_SIZE ? null
                        : MathHelper.abs(playerPos.getZ() - blockpos.getZ()) > this.renderDistanceChunks * RENDER_CHUNK_SIZE ? null
                                : this.viewFrustum.getRenderChunk(blockpos);
    }
    
    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;needsUpdate()Z"))
    public boolean doChunkNeedUpdate(RenderChunk renderChunk) {
        return renderChunk.needsUpdate() && world.isAreaLoaded(renderChunk.getPosition(),
                renderChunk.getPosition().add(RENDER_CHUNK_MAX_POS_OFFSET, RENDER_CHUNK_MAX_POS_OFFSET, RENDER_CHUNK_MAX_POS_OFFSET));
    }

    @ModifyConstant(method = "renderWorldBorder", constant = {
            @Constant(doubleValue = 0.0D),
            @Constant(doubleValue = 256.0D)
    }, slice = @Slice(from = @At(value = "HEAD"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V")), require = 2)
    private double renderWorldBorder_getRenderHeight(double original, Entity entity, float partialTicks) {
        return original == 0.0D ? entity.posY - 128 : entity.posY + 128;
    }
    
    @ModifyConstant(method = "setupTerrain", constant = @Constant(doubleValue = 16.0D))
    public double onSetupTerrain1(double oldValue) {
        return Double.MAX_VALUE;
    }

    @ModifyConstant(method = "setupTerrain", constant = @Constant(intValue = 16))
    public int onSetupTerrain2(int oldValue) {
        return RENDER_CHUNK_SIZE;
    }

    @ModifyConstant(method = "getVisibleFacings", constant = @Constant(intValue = 4))
    public int onGetVisibleFacings(int oldValue) {
        return RENDER_CHUNK_SIZE_BIT;
    }

    @ModifyConstant(method = "getVisibleFacings", constant = @Constant(intValue = 15))
    public int onGetVisibleFacings2(int oldValue) {
        return RENDER_CHUNK_MAX_POS_OFFSET;
    }
}
