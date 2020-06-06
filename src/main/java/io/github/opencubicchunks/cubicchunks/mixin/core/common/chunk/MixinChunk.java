package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.Cube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletionException;

@Mixin(Chunk.class)
public abstract class MixinChunk implements IChunk {

    @Shadow @Final private World world;

    @Shadow @Final private ChunkPos pos;

    @Shadow public abstract ChunkStatus getStatus();

    // getBlockState

    @Override public boolean isEmptyBetween(int startY, int endY) {
        return false;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/IFluidState;", "setBlockState"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=get"
            ))
    private ChunkSection getStorage(ChunkSection[] array, int y) {
        ICube cube = getCube(y);
        if (cube instanceof EmptyCube) {
            return null;
        }
        ChunkSection[] cubeSections = cube.getCubeSections();
        return cubeSections[Coords.sectionToIndex(pos.x, y, pos.z)];
    }

    @SuppressWarnings("ConstantConditions")
    private ICube getCube(int y) {
        try {
            return ((ICubeProvider) world.getChunkProvider()).getCube(
                    Coords.sectionToCube(pos.x),
                    Coords.sectionToCube(y),
                    Coords.sectionToCube(pos.z), getStatus(), true);
        } catch (CompletionException ex) {
            // CompletionException here breaks vanilla crash report handler
            // because CompletionException stacktrace doesn't have any part in common
            // with the stacktrace at the moment of it being caught, as it's actually an exception
            // coming from a different thread. To get around it, we re-throw that exception
            // wrapped in an exception that actually comes from here
            throw new RuntimeException("Exception getting cube", ex);
        }
    }

    @ModifyConstant(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/IFluidState;"},
            constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getMinHeight(int _0) {
        return Integer.MIN_VALUE;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/IFluidState;"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=length"
            ))
    private int getStorage(ChunkSection[] array) {
        return Integer.MAX_VALUE;
    }

    // setBlockState

    @Redirect(method = "setBlockState",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=set"
            ))
    private void setStorage(ChunkSection[] array, int y, ChunkSection newVal) {
        ICube cube = getCube(y);
        if (cube instanceof EmptyCube) {
            return;
        }
        cube.getCubeSections()[Coords.sectionToIndex(pos.x, y, pos.z)] = newVal;
    }

    // Entities

    @Redirect(method =
            { "removeEntityAtIndex", "addEntity", "getEntitiesWithinAABBForEntity", "getEntitiesWithinAABBForList", "getEntitiesOfTypeWithinAABB" },
            at = @At(value = "FIELD", target="Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;",
            args="array=get"))
    public ClassInheritanceMultiMap<Entity> getEntityList(ClassInheritanceMultiMap<Entity>[] entityLists, int y)
    {
        Cube cube = (Cube)this.getCube(y);

        if(!(cube instanceof EmptyCube)) {
            return cube.getEntityLists()[Coords.sectionToIndex(this.pos.x, y, this.pos.z)];
        }
        return new ClassInheritanceMultiMap<>(Entity.class);
    }
    @Redirect(method =
            { "addEntity", "removeEntityAtIndex", "getEntitiesWithinAABBForEntity", "getEntitiesWithinAABBForList", "getEntitiesOfTypeWithinAABB" },
            at = @At(
                    value = "FIELD",
                    target="Lnet/minecraft/world/chunk/Chunk;entityLists:[Lnet/minecraft/util/ClassInheritanceMultiMap;",
                    args="array=length"
            ))
    public int getEntityListsLength(ClassInheritanceMultiMap<Entity>[] entityLists) {
        return CubicChunks.worldMAXHeight / 16;
    }
    @ModifyConstant(method = { "addEntity", "removeEntityAtIndex" }, constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO,
            intValue = 0))
    public int getLowerHeightLimit(int _0)
    {
        return -CubicChunks.worldMAXHeight / 16;
    }
    @ModifyConstant(method = { "getEntitiesWithinAABBForEntity", "getEntitiesWithinAABBForList", "getEntitiesOfTypeWithinAABB" },
            constant = { @Constant(intValue = 0, ordinal = 0), @Constant(intValue = 0, ordinal = 1) } )
    public int getLowerClampLimit(int _0)
    {
        return -CubicChunks.worldMAXHeight / 16;
    }
}