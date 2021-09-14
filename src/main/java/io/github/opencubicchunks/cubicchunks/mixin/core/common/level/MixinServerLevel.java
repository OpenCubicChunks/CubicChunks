package io.github.opencubicchunks.cubicchunks.mixin.core.common.level;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.opencubicchunks.cubicchunks.chunk.entity.ChunkEntityStateEventHandler;
import io.github.opencubicchunks.cubicchunks.chunk.entity.ChunkEntityStateEventSource;
import io.github.opencubicchunks.cubicchunks.chunk.entity.IsCubicEntityContext;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicFastServerTickList;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.level.levelgen.heightmap.SurfaceTrackerWrapper;
import io.github.opencubicchunks.cubicchunks.world.server.CubicServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel extends Level implements CubicServerLevel {
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;

    @Shadow @Final private ServerTickList<Fluid> liquidTicks;

    @Shadow @Final private ServerTickList<Block> blockTicks;

    protected MixinServerLevel(WritableLevelData p_i231617_1_, ResourceKey<Level> p_i231617_2_, DimensionType p_i231617_4_,
                               Supplier<ProfilerFiller> p_i231617_5_, boolean p_i231617_6_, boolean p_i231617_7_, long p_i231617_8_) {
        super(p_i231617_1_, p_i231617_2_, p_i231617_4_, p_i231617_5_, p_i231617_6_, p_i231617_7_, p_i231617_8_);
    }

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/ServerTickList"))
    private <T> ServerTickList<T> constructTickList(ServerLevel serverLevel, Predicate<T> predicate, Function<T, ResourceLocation> function,
                                                    Consumer<TickNextTickData<T>> consumer) {
        return new CubicFastServerTickList<>(serverLevel, predicate, function, consumer);
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void markCubic(MinecraftServer minecraftServer, Executor executor, LevelStorageSource.LevelStorageAccess levelStorageAccess, ServerLevelData serverLevelData,
                           ResourceKey<Level> resourceKey, DimensionType dimensionType, ChunkProgressListener chunkProgressListener, ChunkGenerator chunkGenerator, boolean bl, long l,
                           List<CustomSpawner> list, boolean bl2, CallbackInfo ci) {
        ((IsCubicEntityContext) this.entityManager).setIsCubic(((CubicLevelHeightAccessor) this).isCubic());
        if (this.liquidTicks instanceof CubicFastServerTickList) {
            ((ChunkEntityStateEventSource) this.entityManager).registerChunkEntityStateEventHandler((ChunkEntityStateEventHandler) this.liquidTicks);
        }
        if (this.blockTicks instanceof CubicFastServerTickList) {
            ((ChunkEntityStateEventSource) this.entityManager).registerChunkEntityStateEventHandler((ChunkEntityStateEventHandler) this.blockTicks);
        }
    }

    @Redirect(method = "tickChunk", at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I", ordinal = 1))
    private int onRandNextInt(Random random, int bound, LevelChunk levelChunk, int i) {

        if (!((CubicLevelHeightAccessor) levelChunk).isCubic()) {
            return random.nextInt(bound);
        }


        ChunkPos chunkPos = levelChunk.getPos();
        int x = chunkPos.getMinBlockX();
        int z = chunkPos.getMinBlockZ();
        BlockPos pos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.getBlockRandomPos(x, 0, z, 15));
        if (!isInWorldBounds(pos)) {
            return -1;
        }
        if (((CubicLevelAccessor) this).getCube(pos, ChunkStatus.FULL, false) == null) {
            return -1;
        }
        return this.random.nextInt(16);
    }


    @Redirect(method = "isPositionTickingWithEntitiesLoaded", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;asLong(Lnet/minecraft/core/BlockPos;)J"))
    private long useCubePosInCubicWorld(BlockPos blockPos) {
        return ((CubicLevelHeightAccessor) this).isCubic() ? CubePos.asLong(blockPos) : ChunkPos.asLong(blockPos);
    }

    @Override
    public void onCubeUnloading(LevelCube cube) {
        cube.invalidateAllBlockEntities();

        ChunkPos pos = cube.getCubePos().asChunkPos();
        for (int x = 0; x < CubeAccess.DIAMETER_IN_SECTIONS; x++) {
            for (int z = 0; z < CubeAccess.DIAMETER_IN_SECTIONS; z++) {
                // TODO this might cause columns to reload after they've already been unloaded
                LevelChunk chunk = this.getChunk(pos.x + x, pos.z + z);
                for (Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
                    Heightmap heightmap = entry.getValue();
                    SurfaceTrackerWrapper tracker = (SurfaceTrackerWrapper) heightmap;
                    tracker.unloadCube(cube);
                }
            }
        }
    }

    @Override
    public void tickCube(LevelCube cube, int randomTicks) {
        ProfilerFiller profilerFiller = this.getProfiler();

        // TODO lightning and snow/freezing - see ServerLevel.tickChunk()

        profilerFiller.push("tickBlocks");
        if (randomTicks > 0) {
            LevelChunkSection[] sections = cube.getCubeSections();
            CubePos cubePos = cube.getCubePos();
            for (int i = 0; i < sections.length; i++) {
                LevelChunkSection chunkSection = sections[i];
                if (chunkSection != LevelChunk.EMPTY_SECTION && chunkSection.isRandomlyTicking()) {
                    SectionPos columnPos = Coords.sectionPosByIndex(cubePos, i);
                    int minX = columnPos.minBlockX();
                    int minY = columnPos.minBlockY();
                    int minZ = columnPos.minBlockZ();
                    for (int j = 0; j < randomTicks; j++) {
                        BlockPos blockPos = this.getBlockRandomPos(minX, minY, minZ, 15);
                        profilerFiller.push("randomTick");
                        BlockState blockState = chunkSection.getBlockState(blockPos.getX() - minX, blockPos.getY() - minY, blockPos.getZ() - minZ);
                        if (blockState.isRandomlyTicking()) {
                            blockState.randomTick((ServerLevel) (Object) this, blockPos, this.random);
                        }

                        FluidState fluidState = blockState.getFluidState();
                        if (fluidState.isRandomlyTicking()) {
                            fluidState.randomTick(this, blockPos, this.random);
                        }

                        profilerFiller.pop();
                    }
                }
            }
        }
        profilerFiller.pop();
    }
}