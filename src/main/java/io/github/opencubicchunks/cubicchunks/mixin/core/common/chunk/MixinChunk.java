package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ClassInheritanceMultiMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.CompletionException;

import javax.annotation.Nullable;

@Mixin(Chunk.class)
public abstract class MixinChunk implements IChunk {

    @Shadow @Final private World level;

    @Shadow @Final private ChunkPos chunkPos;

    @Shadow public abstract ChunkStatus getStatus();

    // getBlockState

    @Shadow @Final private Map<BlockPos, TileEntity> blockEntities;

    @Shadow @Final private Map<BlockPos, CompoundNBT> pendingBlockEntities;

    @Override public boolean isYSpaceEmpty(int startY, int endY) {
        return false;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/FluidState;", "setBlockState"},
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/chunk/Chunk;sections:[Lnet/minecraft/world/chunk/ChunkSection;",
                    args = "array=get"
            ))
    private ChunkSection getStorage(ChunkSection[] array, int y) {
        IBigCube cube = this.getCube(y);
        if (cube instanceof EmptyCube) {
            return null;
        }
        ChunkSection[] cubeSections = cube.getCubeSections();
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

    @ModifyConstant(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/FluidState;"},
            constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getMinHeight(int _0) {
        return Integer.MIN_VALUE;
    }

    @Redirect(method = {"getBlockState", "getFluidState(III)Lnet/minecraft/fluid/FluidState;"},
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
        IBigCube cube = this.getCube(y);
        if (cube instanceof EmptyCube) {
            return;
        }
        cube.getCubeSections()[Coords.sectionToIndex(chunkPos.x, y, chunkPos.z)] = newVal;
    }

    @Redirect(method = "setBlockState", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;unsaved:Z"))
    private void setIsModifiedFromSetBlockState_Field(Chunk chunk, boolean isModifiedIn, BlockPos pos, BlockState state, boolean isMoving) {
//        if (isColumn) {
            this.getCube(Coords.blockToSection(pos.getY())).setDirty(isModifiedIn);
//        } else {
//            dirty = isModifiedIn;
//        }
    }

    // Entities

    @Redirect(method = {
            "removeEntity(Lnet/minecraft/entity/Entity;I)V",
            "addEntity",
            "getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntities(Lnet/minecraft/entity/EntityType;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntitiesOfClass"
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;entitySections:[Lnet/minecraft/util/ClassInheritanceMultiMap;",
            args = "array=get"))
    public ClassInheritanceMultiMap<Entity> getEntityList(ClassInheritanceMultiMap<Entity>[] entityLists, int y) {
        BigCube cube = (BigCube) this.getCube(y);

        if (!(cube instanceof EmptyCube)) {
            return cube.getEntityLists()[Coords.sectionToIndex(this.chunkPos.x, y, this.chunkPos.z)];
        }
        return new ClassInheritanceMultiMap<>(Entity.class);
    }
    @Inject(method = {
        "removeEntity(Lnet/minecraft/entity/Entity;I)V"
    }, at = @At("RETURN"))
    public void setDirty$removeEntity(Entity entity, int p_76608_2_, CallbackInfo ci) {
        BigCube cube = (BigCube) this.getCube(entity.yChunk);

        if (!(cube instanceof EmptyCube)) {
            cube.setDirty(true);
        }
    }
    @Inject(method = {
        "addEntity"
    }, at = @At("RETURN"))
    public void setDirty$addEntity(Entity entity, CallbackInfo ci) {
        BigCube cube = (BigCube) this.getCube(entity.yChunk);

        if (!(cube instanceof EmptyCube)) {
            cube.setDirty(true);
        }
    }

    @Redirect(method = {
            "removeEntity(Lnet/minecraft/entity/Entity;I)V",
            "addEntity",
            "getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntities(Lnet/minecraft/entity/EntityType;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntitiesOfClass"
    }, at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;entitySections:[Lnet/minecraft/util/ClassInheritanceMultiMap;",
            args = "array=length"))
    public int getEntityListsLength(ClassInheritanceMultiMap<Entity>[] entityLists) {
        return CubicChunks.MAX_SUPPORTED_HEIGHT / 16;
    }

    @ModifyConstant(method = {"addEntity", "removeEntity(Lnet/minecraft/entity/Entity;I)V"},
            constant = @Constant(expandZeroConditions = Constant.Condition.LESS_THAN_ZERO, intValue = 0))
    public int getLowerHeightLimit(int _0) {
        return CubicChunks.MIN_SUPPORTED_HEIGHT / 16;
    }

    @ModifyConstant(method = {
            "getEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Ljava/util/function/Predicate;)V",
            "getEntities(Lnet/minecraft/entity/EntityType;Lnet/minecraft/util/math/AxisAlignedBB;Ljava/util/List;Ljava/util/function/Predicate;)V",
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
            return cube.getTileEntityMap().put((BlockPos) key, (TileEntity) value);
        } else if(map == this.pendingBlockEntities) {
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().put((BlockPos) key, (CompoundNBT) value);
        }
        return map.put(key, value);
    }

    @Redirect(method = "addBlockEntity(Lnet/minecraft/tileentity/TileEntity;)V",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;loaded:Z"))
    private boolean getLoadedFromBlockEntity(Chunk chunk, TileEntity tileEntity) {
        return ((BigCube)this.getCube(Coords.blockToSection(tileEntity.getBlockPos().getY()))).getLoaded();
    }
    @Redirect(method = "removeBlockEntity", at = @At(value = "FIELD", target = "Lnet/minecraft/world/chunk/Chunk;loaded:Z"))
    private boolean getLoadedFromBlockPos(Chunk chunk, BlockPos pos) {
        return ((BigCube)this.getCube(Coords.blockToSection(pos.getY()))).getLoaded();
    }
}