package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.biome.ColumnBiomeContainer;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.ClientSurfaceTracker;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.SurfaceTrackerWrapper;
import io.github.opencubicchunks.cubicchunks.mixin.core.common.world.MixinServerWorld;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class MixinChunk implements ChunkAccess, CubicLevelHeightAccessor {

    @Shadow @Final private Level level;
    @Shadow @Final private ChunkPos chunkPos;

    @Shadow @Final private Map<BlockPos, BlockEntity> blockEntities;
    @Shadow @Final private Map<BlockPos, CompoundTag> pendingBlockEntities;
    @Shadow private ChunkBiomeContainer biomes;

    @Shadow public abstract ChunkStatus getStatus();

    @Shadow @Final private LevelChunkSection[] sections;

    @Shadow private volatile boolean unsaved;

    private Boolean isCubic;
    private Boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Shadow protected abstract boolean isInLevel();

    @Override public boolean isYSpaceEmpty(int startY, int endY) {
        return false;
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;"
            + "Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/level/TickList;Lnet/minecraft/world/level/TickList;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;"
            + "Ljava/util/function/Consumer;)V",
        at = @At("RETURN"))
    private void onInit(Level levelIn, ChunkPos pos, ChunkBiomeContainer chunkBiomeContainer, UpgradeData upgradeData, TickList<Block> tickList, TickList<Fluid> tickList2, long l,
                        LevelChunkSection[] levelChunkSections, Consumer<LevelChunk> consumer, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) level).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) level).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) level).worldStyle();

        if (!this.isCubic()) {
            return;
        }

        //TODO: is cubicworld
        //Client will already supply a ColumnBiomeContainer, server will not
        if (!(biomes instanceof ColumnBiomeContainer)) {
            this.biomes = new ColumnBiomeContainer(levelIn.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), levelIn, levelIn);
        }
    }

    @Redirect(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;"
            + "Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/level/TickList;Lnet/minecraft/world/level/TickList;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;"
            + "Ljava/util/function/Consumer;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getSectionsCount()I"))
    private int getFakeSectionCount(Level levelIn) {
        if (!((CubicLevelHeightAccessor) levelIn).isCubic()) { // The fields are not initialized here yet.
            return levelIn.getSectionsCount();
        }

        return 16; // TODO: properly handle Chunk
    }

    @Redirect(
        method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;"
            + "Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/level/TickList;Lnet/minecraft/world/level/TickList;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;"
            + "Ljava/util/function/Consumer;)V",
        at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/Heightmap"))
    private Heightmap getCCHeightmap(ChunkAccess chunkAccess, Heightmap.Types type) {
        if (!((CubicLevelHeightAccessor) this.level).isCubic()) { // The fields are not initialized here yet.
            return new Heightmap(chunkAccess, type);
        }

        if (this.level.isClientSide()) {
            return new ClientSurfaceTracker(chunkAccess, type);
        } else {
            return new SurfaceTrackerWrapper(chunkAccess, type); //TODO: Load from this from files.
        }
    }

    @Redirect(method = { "getBlockState", "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;", "setBlockState" },
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;sections:[Lnet/minecraft/world/level/chunk/LevelChunkSection;",
            args = "array=get"
        ))
    private LevelChunkSection getStorage(LevelChunkSection[] array, int sectionIndex) {
        if (!this.isCubic()) {
            return array[sectionIndex];
        }

        int sectionY = getSectionYFromSectionIndex(sectionIndex);
        IBigCube cube = this.getCube(sectionY);
        if (cube instanceof EmptyCube) {
            return null;
        }
        LevelChunkSection[] cubeSections = cube.getCubeSections();
        return cubeSections[Coords.sectionToIndex(chunkPos.x, sectionY, chunkPos.z)];
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

    @ModifyConstant(method = { "getBlockState", "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;" },
        constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getMinHeight(int _0) {
        if (!this.isCubic()) {
            return _0;
        }

        return Integer.MIN_VALUE;
    }

    @Redirect(method = { "getBlockState", "getFluidState(III)Lnet/minecraft/world/level/material/FluidState;" },
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;sections:[Lnet/minecraft/world/level/chunk/LevelChunkSection;",
            args = "array=length"
        ))
    private int getStorage(LevelChunkSection[] array) {
        if (!this.isCubic()) {
            return array.length;
        }

        return Integer.MAX_VALUE;
    }

    // setBlockState

    @Redirect(method = "setBlockState",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/level/chunk/LevelChunk;sections:[Lnet/minecraft/world/level/chunk/LevelChunkSection;",
            args = "array=set"
        ))
    private void setStorage(LevelChunkSection[] array, int sectionIndex, LevelChunkSection newVal) {
        if (!this.isCubic()) {
            array[sectionIndex] = newVal;
            return;
        }

        int sectionY = getSectionYFromSectionIndex(sectionIndex);
        IBigCube cube = this.getCube(sectionY);
        if (cube instanceof EmptyCube) {
            return;
        }
        cube.getCubeSections()[Coords.sectionToIndex(chunkPos.x, sectionY, chunkPos.z)] = newVal;
    }

    @Redirect(method = "setBlockState", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/chunk/LevelChunk;unsaved:Z"))
    private void setIsModifiedFromSetBlockState_Field(LevelChunk chunk, boolean isModifiedIn, BlockPos pos, BlockState state, boolean isMoving) {
        if (!this.isCubic()) {
            this.unsaved = isModifiedIn;
            return;
        }


//        if (isColumn) {
        this.getCube(Coords.blockToSection(pos.getY())).setDirty(isModifiedIn);
//        } else {
//            dirty = isModifiedIn;
//        }
    }

    //This should return object because Hashmap.get also does
    @SuppressWarnings({ "rawtypes", "UnresolvedMixinReference" })
    @Redirect(method = "*",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object getTileEntity(Map map, Object key) {
        if (map == this.blockEntities) {
            if (!this.isCubic()) {
                return map.get(key);
            }
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getTileEntityMap().get(key);
        } else if (map == this.pendingBlockEntities) {
            if (!this.isCubic()) {
                return map.get(key);
            }
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().get(key);
        }
        return map.get(key);
    }

    @SuppressWarnings({ "rawtypes", "UnresolvedMixinReference" }) @Nullable
    @Redirect(
        method = "*",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object removeTileEntity(Map map, Object key) {
        if (map == this.blockEntities) {
            if (!this.isCubic()) {
                return map.remove(key);
            }
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getTileEntityMap().remove(key);
        } else if (map == this.pendingBlockEntities) {
            if (!this.isCubic()) {
                return map.remove(key);
            }
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().remove(key);
        }
        return map.remove(key);
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "UnresolvedMixinReference" }) @Nullable
    @Redirect(method = "*",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object putTileEntity(Map map, Object key, Object value) {
        if (map == this.blockEntities) {
            if (!this.isCubic()) {
                return map.put(key, value);
            }
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getTileEntityMap().put((BlockPos) key, (BlockEntity) value);
        } else if (map == this.pendingBlockEntities) {
            if (!this.isCubic()) {
                return map.put(key, value);
            }
            BigCube cube = (BigCube) this.getCube(Coords.blockToSection(((BlockPos) key).getY()));
            return cube.getDeferredTileEntityMap().put((BlockPos) key, (CompoundTag) value);
        }
        return map.put(key, value);
    }

    @Redirect(method = "addAndRegisterBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;isInLevel()Z"))
    private boolean getLoadedFromBlockEntity(LevelChunk chunk, BlockEntity tileEntity) {
        if (!this.isCubic()) {
            return this.isInLevel();
        }

        return ((BigCube) this.getCube(Coords.blockToSection(tileEntity.getBlockPos().getY()))).isInLevel();
    }

    @Redirect(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;isInLevel()Z"))
    private boolean getLoadedFromBlockPos(LevelChunk chunk, BlockPos pos) {
        if (!this.isCubic()) {
            return this.isInLevel();
        }

        return ((BigCube) this.getCube(Coords.blockToSection(pos.getY()))).isInLevel();
    }

    @Override public WorldStyle worldStyle() {
                if (worldStyle == null)
            new Error().printStackTrace();
        return worldStyle;
    }

    @Override public Boolean isCubic() {
                if (isCubic == null)
            new Error().printStackTrace();
        return isCubic;
    }

    @Override public Boolean generates2DChunks() {
                if (generates2DChunks == null)
            new Error().printStackTrace();
        return generates2DChunks;
    }

}