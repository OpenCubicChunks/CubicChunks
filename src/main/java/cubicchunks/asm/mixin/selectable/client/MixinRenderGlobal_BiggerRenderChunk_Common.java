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

import static cubicchunks.client.RenderConstants.*;

import java.util.Iterator;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

import cubicchunks.client.CubeProviderClient;
import cubicchunks.client.VisGraphBigChunkAdapted;
import cubicchunks.util.Coords;
import cubicchunks.world.ICubicWorldClient;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

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

    /*
     * @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;hasNext()Z", remap = false)) public boolean onIteratingThruRenderInfos(Iterator<RenderGlobal.ContainerLocalRenderInformation> renderInfosIterator, Entity renderViewEntity, ICamera camera, float partialTicks) { ICubicWorldClient cworld = (ICubicWorldClient) world; if (cworld.isCubicWorld()) return false; return renderInfosIterator.hasNext(); }
     */

    ClassInheritanceMultiMap<Entity> mutableEntityMapWrapper = new ClassInheritanceMultiMap<Entity>(Entity.class);
    @SuppressWarnings("unchecked") ClassInheritanceMultiMap<Entity>[] dummyEntityStorageArray = new ClassInheritanceMultiMap[1];

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;getPosition()Lnet/minecraft/util/math/BlockPos;"))
    public BlockPos onGetPosition(RenderChunk renderChunk) {
        mutableEntityMapWrapper.values.clear();
        BlockPos rcpos = renderChunk.getPosition();
        int cx0 = Coords.blockToCube(rcpos.getX());
        int cy0 = Coords.blockToCube(rcpos.getY());
        int cz0 = Coords.blockToCube(rcpos.getZ());
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        if (cworld.isCubicWorld()) {
            CubeProviderClient cubeProvider = cworld.getCubeCache();
            for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES; cy++)
                    for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                        mutableEntityMapWrapper.values.addAll(cubeProvider.getCube(cx, cy, cz).getEntityContainer().getEntitySet());
                    }
        } else {
            for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                    ClassInheritanceMultiMap<Entity>[] entityLists = world.getChunkFromChunkCoords(cx, cz).getEntityLists();
                    for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES && cy < 16 && cy >= 0; cy++) {
                        mutableEntityMapWrapper.values.addAll(entityLists[cy].values);
                    }
                }
        }
        return BlockPos.ORIGIN;
    }

    @Redirect(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getEntityLists()[Lnet/minecraft/util/ClassInheritanceMultiMap;"))
    public ClassInheritanceMultiMap<Entity>[] onGettingClassInheritanceMultiMap(Chunk chunk) {
        dummyEntityStorageArray[0] = mutableEntityMapWrapper;
        return dummyEntityStorageArray;
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

    @ModifyConstant(method = "renderWorldBorder", constant = {
            @Constant(doubleValue = 0.0D),
            @Constant(doubleValue = 256.0D)
    }, slice = @Slice(from = @At(value = "HEAD"), to = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Tessellator;draw()V")), require = 2)
    private double renderWorldBorder_getRenderHeight(double original, Entity entity, float partialTicks) {
        return original == 0.0D ? entity.posY - 128 : entity.posY + 128;
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/RenderChunk;needsUpdate()Z"))
    public boolean doChunkNeedUpdate(RenderChunk renderChunk) {
        return renderChunk.needsUpdate() && world.isAreaLoaded(renderChunk.getPosition(),
                renderChunk.getPosition().add(RENDER_CHUNK_MAX_POS, RENDER_CHUNK_MAX_POS, RENDER_CHUNK_MAX_POS));
    }

    @ModifyConstant(method = "setupTerrain", constant = @Constant(doubleValue = 16.0D))
    public double onSetupTerrain1(double oldValue) {
        return Double.MAX_VALUE;
    }

    @ModifyConstant(method = "setupTerrain", constant = @Constant(intValue = 16))
    public int onSetupTerrain2(int oldValue) {
        return RENDER_CHUNK_SIZE;
    }

    /**
     * @author Foghrye4
     * @reason function is short, simple and does not allow to use bigger render chunks.
     */
    @Overwrite
    private Set<EnumFacing> getVisibleFacings(BlockPos pos) {
        VisGraphBigChunkAdapted visgraph = new VisGraphBigChunkAdapted();
        int bx = pos.getX() & RENDER_CHUNK_START_POS_MASK;
        int by = pos.getY() & RENDER_CHUNK_START_POS_MASK;
        int bz = pos.getZ() & RENDER_CHUNK_START_POS_MASK;
        int cx0 = Coords.blockToCube(bx);
        int cy0 = Coords.blockToCube(by);
        int cz0 = Coords.blockToCube(bz);
        ICubicWorldClient cworld = (ICubicWorldClient) world;
        if (cworld.isCubicWorld()) {
            CubeProviderClient cubeProvider = cworld.getCubeCache();
            for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES; cy++)
                    for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                        ExtendedBlockStorage ebs = cubeProvider.getCube(cx, cy, cz).getStorage();
                        this.addAllOpaqueBlocksToVisGraph(visgraph, ebs);
                    }
        } else {
            for (int cx = cx0; cx < cx0 + RENDER_CHUNK_SIZE_IN_CUBES; cx++)
                for (int cz = cz0; cz < cz0 + RENDER_CHUNK_SIZE_IN_CUBES; cz++) {
                    ExtendedBlockStorage[] ebsArrays = world.getChunkFromChunkCoords(cx, cz).getBlockStorageArray();
                    for (int cy = cy0; cy < cy0 + RENDER_CHUNK_SIZE_IN_CUBES && cy < 16 && cy >= 0; cy++) {
                        ExtendedBlockStorage ebs = ebsArrays[cy];
                        this.addAllOpaqueBlocksToVisGraph(visgraph, ebs);
                    }
                }
        }
        return visgraph.getVisibleFacings(pos);
    }

    private void addAllOpaqueBlocksToVisGraph(VisGraphBigChunkAdapted visgraph, ExtendedBlockStorage ebs) {
        if (ebs == null || ebs.isEmpty())
            return;
        for (int lx = 0; lx < Cube.SIZE; lx++)
            for (int ly = 0; ly < Cube.SIZE; ly++)
                for (int lz = 0; lz < Cube.SIZE; lz++) {
                    IBlockState bstate = ebs.get(lx, ly, lz);
                    if (bstate.isOpaqueCube())
                        visgraph.setOpaqueCube(lx, ly, lz);
                }
    }
}
