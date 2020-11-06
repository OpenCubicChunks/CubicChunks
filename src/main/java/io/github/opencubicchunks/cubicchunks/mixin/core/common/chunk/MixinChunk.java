package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

@Mixin(LevelChunk.class)
public abstract class MixinChunk implements ChunkAccess {

    @Shadow @Final private Level level;

    @Shadow @Final private ChunkPos chunkPos;

    @Shadow public abstract ChunkStatus getStatus();

    // getBlockState

    @Shadow @Final private Map<BlockPos, BlockEntity> blockEntities;

    @Shadow @Final private Map<BlockPos, CompoundTag> pendingBlockEntities;

    @Override public boolean isYSpaceEmpty(int startY, int endY) {
        return false;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", "setBlockState"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;sections:[Lnet/minecraft/world/level/chunk/LevelChunkSection;",
                    args = "array=get"
            ))
    private LevelChunkSection getStorage(LevelChunkSection[] array, int y) {
        IBigCube cube = this.getCube(y);
        if (cube instanceof EmptyCube) {
            return null;
        }
        LevelChunkSection[] cubeSections = cube.getCubeSections();
        return cubeSections[Coords.sectionToIndex(chunkPos.x, y, chunkPos.z)];
    }

    @SuppressWarnings("ConstantConditions")
    private IBigCube getCube(int y) {
        try {
            return ((ICubeProvider) level.getChunkSource()).getCube(
                    Coords.sectionToCube(chunkPos.x),
                    Coords.sectionToCube(y),
                    Coords.sectionToCube(chunkPos.z), getStatus(), true);
        } catch (CompletionException ex) {
            // CompletionException here breaks vanilla crash report handler
            // because CompletionException stacktrace doesn't have any part in common
            // with the stacktrace at the moment of it being caught, as it's actually an exception
            // coming from a different thread. To get around it, we re-throw that exception
            // wrapped in an exception that actually comes from here
            throw new RuntimeException("Exception getting cube", ex);
        }
    }

    @ModifyConstant(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;"},
            constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getMinHeight(int _0) {
        return Integer.MIN_VALUE;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;sections:[Lnet/minecraft/world/level/chunk/LevelChunkSection;",
                    args = "array=length"
            ))
    private int getStorage(LevelChunkSection[] array) {
        return Integer.MAX_VALUE;
    }

    // setBlockState

    @Redirect(method = "setBlockState",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/chunk/LevelChunk;sections:[Lnet/minecraft/world/level/chunk/LevelChunkSection;",
                    args = "array=set"
            ))
    private void setStorage(LevelChunkSection[] array, int y, LevelChunkSection newVal) {
        IBigCube cube = this.getCube(y);
        if (cube instanceof EmptyCube) {
            return;
        }
        cube.getCubeSections()[Coords.sectionToIndex(chunkPos.x, y, chunkPos.z)] = newVal;
    }

    @Redirect(method = "setBlockState", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;unsaved:Z"))
    private void setIsModifiedFromSetBlockState_Field(LevelChunk chunk, boolean isModifiedIn, BlockPos pos, BlockState state, boolean isMoving) {
//        if (isColumn) {
            this.getCube(Coords.blockToSection(pos.getY())).setDirty(isModifiedIn);
//        } else {
//            dirty = isModifiedIn;
//        }
    }

    // Entities

    @Redirect(method = {
            "removeEntity(Lnet/minecraft/world/entity/Entity;I)V",
            "addEntity",
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntities(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/phys/AABB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntitiesOfClass"
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;entitySections:[Lnet/minecraft/util/ClassInstanceMultiMap;",
            args = "array=get"))
    public ClassInstanceMultiMap<Entity> getEntityList(ClassInstanceMultiMap<Entity>[] entityLists, int y) {
        BigCube cube = (BigCube) this.getCube(y);

        if (!(cube instanceof EmptyCube)) {
            return cube.getEntityLists()[Coords.sectionToIndex(this.chunkPos.x, y, this.chunkPos.z)];
        }
        return new ClassInstanceMultiMap<>(Entity.class);
    }

    @Redirect(method = {
            "removeEntity(Lnet/minecraft/world/entity/Entity;I)V",
            "addEntity",
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntities(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/phys/AABB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntitiesOfClass"
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;entitySections:[Lnet/minecraft/util/ClassInstanceMultiMap;",
            args = "array=length"))
    public int getEntityListsLength(ClassInstanceMultiMap<Entity>[] entityLists) {
        return CubicChunks.MAX_SUPPORTED_HEIGHT / 16;
    }

    @ModifyConstant(method = {"addEntity", "removeEntity(Lnet/minecraft/world/entity/Entity;I)V"},
            constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0))
    public int getLowerHeightLimit(int _0) {
        return CubicChunks.MIN_SUPPORTED_HEIGHT / 16;
    }

    @ModifyConstant(method = {
            "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntities(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/phys/AABB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntitiesOfClass"
    }, constant = {@Constant(intValue = 0, ordinal = 0), @Constant(intValue = 0, ordinal = 1)})
    public int getLowerClampLimit(int _0) {
        return CubicChunks.MIN_SUPPORTED_HEIGHT / 16;
    }

    //This should return object because Hashmap.get also does
    @SuppressWarnings({"rawtypes", "UnresolvedMixinReference"})
    @Redirect(method = "*",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object getTileEntity(Map map, Object key) {
        if(map == this.blockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getTileEntityMap().get(key);
        } else if(map == this.pendingBlockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().get(key);
        }
        return map.get(key);
    }

    @SuppressWarnings({"rawtypes", "UnresolvedMixinReference"}) @Nullable
    @Redirect(
            method = "*",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object removeTileEntity(Map map, Object key) {
        if(map == this.blockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getTileEntityMap().remove(key);
        }else if(map == this.pendingBlockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().remove(key);
        }
        return map.remove(key);
    }

    @SuppressWarnings({"rawtypes", "unchecked", "UnresolvedMixinReference"}) @Nullable
    @Redirect(method = "*",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object putTileEntity(Map map, Object key, Object value) {
        if(map == this.blockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getTileEntityMap().put((BlockPos) key, (BlockEntity) value);
        } else if(map == this.pendingBlockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().put((BlockPos) key, (CompoundTag) value);
        }
        return map.put(key, value);
    }

    @Redirect(method = "addBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;loaded:Z"))
    private boolean getLoadedFromBlockEntity(LevelChunk chunk, BlockEntity tileEntity) {
        return ((BigCube)this.getCube(Coords.blockToSection(tileEntity.getBlockPos().getY()))).getLoaded();
    }
    @Redirect(method = "removeBlockEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;loaded:Z"))
    private boolean getLoadedFromBlockPos(LevelChunk chunk, BlockPos pos) {
        return ((BigCube)this.getCube(Coords.blockToSection(pos.getY()))).getLoaded();
    }
}