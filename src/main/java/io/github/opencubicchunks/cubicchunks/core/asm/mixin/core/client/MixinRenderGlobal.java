/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.core.world.EntityContainer;
import io.github.opencubicchunks.cubicchunks.core.world.cube.BlankCube;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Fixes renderEntities crashing when rendering cubes
 * that are not at existing array index in chunk.getEntityLists(),
 * <p>
 * Allows to render cubes outside of 0..256 height range.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Nullable private BlockPos position;

    @Shadow private int renderDistanceChunks;

    @Shadow private ViewFrustum viewFrustum;

    /*
     * This allows to get the Y position of rendered entity by injecting itself directly before call to
     * chunk.getEntityLists
     */
    @Group(name = "renderEntitiesFix", min = 3, max = 3)
    @Inject(method = "renderEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/WorldClient;getChunk(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/world/chunk/Chunk;"),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    public void onGetPosition(Entity renderViewEntity, ICamera camera, float partialTicks,
            CallbackInfo ci, int pass, double d0, double d1, double d2,
            Entity entity, double d3, double d4, double d5,
            List<Entity> list, List<Entity> list1, List<Entity> list2,
            BlockPos.PooledMutableBlockPos pos, Iterator<?> var21,
            /*RenderGlobal.ContainerLocalRenderInformation*/ @Coerce Object info) {
        RenderChunk renderChunk = ((IContainerLocalRenderInformation) info).getRenderChunk();
        ICubicWorld world = (ICubicWorld) renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    /*
     * Optifine-specific version of the entity render fix. Versions 1.12.2_HD_U_C7_pre to 1.12.2_HD_U_F5
     *
     * This method sets position for MixinRenderGlobal#getRenderChunkYPos to use
     */
    @Dynamic @Group(name = "renderEntitiesFix") @Inject(method = "renderEntities",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;",
            remap = false),
        locals = LocalCapture.CAPTURE_FAILSOFT, remap = true)
    public void onGetPosition(Entity renderViewEntity, ICamera camera, float partialTicks,
        CallbackInfo ci, int pass, double d0, double d1, double d2,
        Entity entity, double d3, double d4, double d5,
        List list, boolean forgeEntityPass, boolean forgeTileEntityPass, boolean isShaders, boolean oldFancyGraphics, List list1, List list2,
        BlockPos.PooledMutableBlockPos pos, Iterator<?> var22,
        /*RenderGlobal.ContainerLocalRenderInformation*/ @Coerce Object info) {
        RenderChunk renderChunk = ((IContainerLocalRenderInformation) info).getRenderChunk();
        ICubicWorld world = (ICubicWorld) renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    /*
     * Optifine-specific version of the entity render fix. Versions 1.12.2_HD_U_G5 and up
     *
     * This method sets position for MixinRenderGlobal#getRenderChunkYPos to use
     */
    @Dynamic @Group(name = "renderEntitiesFix")
    @Inject(method = "renderEntities",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getChunk()Lnet/minecraft/world/chunk/Chunk;",
            remap = false),
        locals = LocalCapture.CAPTURE_FAILSOFT, remap = true)
    public void onGetPosition(Entity renderViewEntity, ICamera camera, float partialTicks,
        CallbackInfo ci, int pass, double d0, double d1, double d2,
        Entity entity, double d3, double d4, double d5,
        List list, boolean forgeEntityPass, boolean forgeTileEntityPass, boolean isShaders, List list1, List list2,
        BlockPos.PooledMutableBlockPos pos, boolean playerShadowPass, Iterator<?> var22,
        /*RenderGlobal.ContainerLocalRenderInformation*/ @Coerce Object info) {
        RenderChunk renderChunk = ((IContainerLocalRenderInformation) info).getRenderChunk();
        ICubicWorld world = (ICubicWorld) renderChunk.getWorld();
        if (world.isCubicWorld()) {
            this.position = renderChunk.getPosition();
        } else {
            this.position = null;
        }
    }

    /*
     * After chunk.getEntityLists() renderGlobal needs to get correct element of the array.
     * The array element number is calculated using renderChunk.getPosition().getY() / 16.
     * getY() is redirected to this method to always return 0.
     * <p>
     * Then chunk.getEntityLists is redirected to a method that returns a 1-element array.
     */
    @Group(name = "renderEntitiesFix")
    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I"), require = 1)
    private int getRenderChunkYPos(BlockPos pos) {
        //position is null when it's not cubic chunks renderer
        if (this.position != null) { // also set from optifine specific mixins
            return 0;//must be 0 (or anything between 0 and 15)
        }
        return pos.getY();
    }

    /*
     * Return a 1-element array for Cubic Chunks world,
     * or original chunk.getEntityLists if not cubic chunks world.
     */
    @SuppressWarnings("unchecked")
    @Group(name = "renderEntitiesFix")
    @Redirect(method = "renderEntities",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getEntityLists()[Lnet/minecraft/util/ClassInheritanceMultiMap;"),
            require = 1)
    private ClassInheritanceMultiMap<Entity>[] getEntityList(Chunk chunk) {
        if (position == null) {
            return chunk.getEntityLists(); //TODO: is this right?
        }

        ICube cube = ((IColumn) chunk).getCube(Coords.blockToCube(position.getY()));
        if (cube instanceof BlankCube) {
            return EntityContainer.EMPTY_ARR;
        }

        return new ClassInheritanceMultiMap[]{cube.getEntitySet()};
    }

    @ModifyConstant(
            method = "renderWorldBorder",
            constant = {
                    @Constant(doubleValue = 0.0D),
                    @Constant(doubleValue = 256.0D)
            },
            slice = @Slice(from = @At(value = "HEAD"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V")), require = 2)
    private double renderWorldBorder_getRenderHeight(double original, Entity entity, float partialTicks) {
        return original == 0.0D ? entity.posY - 128 : entity.posY + 128;
    }
}
