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
package cubicchunks.asm.mixin.core.common;

import static cubicchunks.asm.JvmNames.WORLD_GET_LIGHT_WITH_FLAG;
import static cubicchunks.asm.JvmNames.WORLD_IS_AREA_LOADED;
import static cubicchunks.asm.JvmNames.WORLD_IS_BLOCK_LOADED_Z;
import static cubicchunks.asm.JvmNames.WORLD_IS_CHUNK_LOADED;
import static cubicchunks.util.Coords.blockToCube;
import static cubicchunks.util.Coords.cubeToMinBlock;

import cubicchunks.asm.MixinUtils;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.BlankCube;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Contains fixes for hardcoded height checks and other height-related issues.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(World.class)
public abstract class MixinWorld_HeightLimits implements ICubicWorld {

    @Shadow private int skylightSubtracted;

    @Shadow @Final public WorldProvider provider;

    @Shadow public abstract Chunk getChunkFromBlockCoords(BlockPos pos);

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    @Shadow public abstract boolean isBlockLoaded(BlockPos pos);

    @Shadow protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);

    /**
     * This @Overwrite allows World to "see" blocks outside of 0..255 height range.
     *
     * @author Barteks2x
     * @reason It's very simple method and this seems to be the cleanest way to modify it.
     */
    @Overwrite
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return pos.getY() >= getMaxHeight() || pos.getY() < getMinHeight();
    }

    /**
     * @author Barteks2x
     * @reason Replace {@link World#getLight(BlockPos)} with method that works outside of 0..255 height range. It would
     * be possible to fix it using @Redirect and @ModifyConstant but this way is much cleaner, especially for simple
     * method. A @{@link ModifyConstant} wouldn't work because it can't replace comparison to 0. This is because there
     * is a special instruction to compare something to 0, so the constant is never used.
     * <p>
     * Note: The getLight method is used in parts of game logic and entity rendering code. Doesn't directly affect block
     * rendering.
     */
    @Overwrite
    public int getLight(BlockPos pos) {
        if (pos.getY() < this.getMinHeight()) {
            return 0;
        }
        if (pos.getY() >= this.getMaxHeight()) {
            //CubicChunks edit
            //return default light value above maxHeight instead of the same value as at maxHeight
            return EnumSkyBlock.SKY.defaultLightValue;
            //CubicChunks end
        }
        return this.getChunkFromBlockCoords(pos).getLightSubtracted(pos, 0);
    }

    /**
     * This getLight method is used in parts of game logic and entity rendering code.
     * Doesn't directly affect block rendering.
     */
    @Group(name = "getLightHeightOverride", max = 4)
    @ModifyConstant(
            method = "getLight(Lnet/minecraft/util/math/BlockPos;Z)I",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, ordinal = 0),
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/BlockPos;getY()I"),
                    to = @At(value = "INVOKE",
                            target = "Lnet/minecraft/world/World;getChunkFromBlockCoords(Lnet/minecraft/util/math/BlockPos;)"
                                    + "Lnet/minecraft/world/chunk/Chunk;")
            ))
    private int getLightGetYReplace(int zero) {
        return getMinHeight();
    }

    /**
     * Modify constant 255 in {@link World#getLight(BlockPos)} used in case tha height check didn't pass.
     * When max height is exceeded vanilla clamps the value to 255 (maxHeight - 1 = actual max allowed block Y).
     */
    @Group(name = "getLightHeightOverride")
    @ModifyConstant(method = "getLight(Lnet/minecraft/util/math/BlockPos;Z)I",
            constant = {@Constant(intValue = 255), @Constant(intValue = 256)}, require = 2)
    private int getLightGetReplacementYTooHigh(int original) {
        return this.getMaxHeight() + original - 256;
    }

    /**
     * Redirect 0 constant in getLightFor(EnumSkyBlock, BlockPos)
     * so that getLightFor returns light at y=minHeight when below minHeight.
     */
    @Group(name = "getLightForHeightOverride", min = 2, max = 2)
    @ModifyConstant(method = "getLightFor",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.LESS_THAN_ZERO))
    private int getLightForGetMinYReplace(int origY) {
        return this.getMinHeight();
    }

    /**
     * Conditionally replaces isAreaLoaded with Cubic Chunks implementation
     * (continues with vanilla code if it's not a cubic chunks world).
     * World.isAreaLoaded is used to check if some things can be updated (like light).
     * If it returns false - update doesn't happen. This fixes it
     * <p>
     * NOTE: there are some methods that use it incorrectly
     * ie. by checking it at some constant height (usually 0 or 64).
     * These places need to be modified.
     *
     * @author Barteks2x
     */
    @Group(name = "isLoaded", max = 1)
    @Inject(method = WORLD_IS_AREA_LOADED, at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void isAreaLoadedInject(int xStart, int yStart, int zStart, int xEnd, int yEnd, int zEnd, boolean allowEmpty,
            @Nonnull CallbackInfoReturnable<Boolean> cbi) {
        if (!this.isCubicWorld()) {
            return;
        }

        boolean ret = (this.isRemote() && allowEmpty) || // on the client all cubes count as loaded if allowEmpty
                this.testForCubes(
                        xStart, yStart, zStart,
                        xEnd, yEnd, zEnd,
                        Objects::nonNull);

        cbi.cancel();
        cbi.setReturnValue(ret);
    }

    // NOTE: This may break some things

    /**
     * @author Barteks2x
     * @reason CubicChunks needs to check if cube is loaded instead of chunk
     */
    @Inject(method = WORLD_IS_BLOCK_LOADED_Z, cancellable = true, at = @At(value = "HEAD"))
    public void isBlockLoaded(BlockPos pos, boolean allowEmpty, CallbackInfoReturnable<Boolean> cbi) {
        if (!isCubicWorld()) {
            return;
        }
        Cube cube = this.getCubeCache().getLoadedCube(blockToCube(pos.getX()), blockToCube(pos.getY()), blockToCube(pos.getZ()));
        if (allowEmpty) {
            cbi.setReturnValue(cube != null);
        } else {
            cbi.setReturnValue(cube != null && !(cube instanceof BlankCube));
        }
    }

    @Redirect(method = "spawnEntity", at = @At(value = "INVOKE", target = WORLD_IS_CHUNK_LOADED))
    private boolean spawnEntity_isChunkLoaded(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent) {
        assert this == (Object) world;
        if (isCubicWorld()) {
            return this.isBlockLoaded(new BlockPos(cubeToMinBlock(chunkX), ent.posY, cubeToMinBlock(chunkZ)), allowEmpty);
        } else {
            return this.isChunkLoaded(chunkX, chunkZ, allowEmpty);
        }
    }

    @Redirect(method = "updateEntityWithOptionalForce", at = @At(value = "INVOKE", target = WORLD_IS_CHUNK_LOADED, ordinal = 0))
    private boolean updateEntityWithOptionalForce_isChunkLoaded0(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent, boolean force) {
        assert this == (Object) world;
        if (isCubicWorld()) {
            return this.isBlockLoaded(new BlockPos(cubeToMinBlock(chunkX), cubeToMinBlock(ent.chunkCoordY), cubeToMinBlock(chunkZ)), allowEmpty);
        } else {
            return this.isChunkLoaded(chunkX, chunkZ, allowEmpty);
        }
    }

    @Redirect(method = "updateEntityWithOptionalForce", at = @At(value = "INVOKE", target = WORLD_IS_CHUNK_LOADED, ordinal = 1))
    private boolean updateEntityWithOptionalForce_isChunkLoaded1(World world, int chunkX, int chunkZ, boolean allowEmpty, Entity ent, boolean force) {
        assert this == (Object) world;
        if (isCubicWorld()) {
            return this.isBlockLoaded(new BlockPos(cubeToMinBlock(chunkX), ent.posY, cubeToMinBlock(chunkZ)), allowEmpty);
        } else {
            return this.isChunkLoaded(chunkX, chunkZ, allowEmpty);
        }
    }

    private int updateEntities_enityChunkBlockY;

    @Inject(method = "updateEntities",
            at = @At(value = "INVOKE", target = WORLD_IS_CHUNK_LOADED, ordinal = 0),
            locals = LocalCapture.CAPTURE_FAILHARD,
            require = 1)
    private void updateEntities_isChunkLoaded0_getLocals(CallbackInfo cbi, int i, Entity entity, int chunkX, int chunkZ) {
        updateEntities_enityChunkBlockY = cubeToMinBlock(entity.chunkCoordY);
    }

    @Inject(method = "updateEntities",
            at = @At(value = "INVOKE", target = WORLD_IS_CHUNK_LOADED, ordinal = 1),
            locals = LocalCapture.CAPTURE_FAILHARD,
            require = 1)
    private void updateEntities_isChunkLoaded1_getLocals(CallbackInfo cbi, int i, Entity entity, Entity ridingEntity, int chunkX, int chunkZ) {
        updateEntities_enityChunkBlockY = cubeToMinBlock(entity.chunkCoordY);
    }

    @Redirect(method = "updateEntities", at = @At(value = "INVOKE", target = WORLD_IS_CHUNK_LOADED, ordinal = 1))
    private boolean updateEntities_isChunkLoaded(World world, int chunkX, int chunkZ, boolean allowEmpty) {
        assert this == (Object) world;
        if (isCubicWorld()) {
            return this.isBlockLoaded(new BlockPos(cubeToMinBlock(chunkX), updateEntities_enityChunkBlockY, cubeToMinBlock(chunkZ)), allowEmpty);
        } else {
            return this.isChunkLoaded(chunkX, chunkZ, allowEmpty);
        }
    }
}
