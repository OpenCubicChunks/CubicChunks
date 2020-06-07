package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.server;

import com.mojang.datafixers.util.Pair;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting.MixinWorldLightManager;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorldLightManager;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.IntSupplier;

@Mixin(ServerWorldLightManager.class)
public abstract class MixinServerWorldLightManager extends MixinWorldLightManager implements IServerWorldLightManager {

    private CubeTaskPriorityQueueSorter cubeTaskPriorityQueueSorter;
    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor;

    @Shadow @Final private ChunkManager chunkManager;

    @Shadow @Final private ObjectList<Pair<ServerWorldLightManager.Phase, Runnable>> field_215606_c;

    @Shadow private volatile int field_215609_f;

    @Shadow protected abstract void func_215603_b();

    /**
     * @author NotStirred
     * @reason lambdas
     */
    @Overwrite
    public void checkBlock(BlockPos blockPosIn)
    {
        BlockPos blockpos = blockPosIn.toImmutable();
        this.schedulePhaseTask(Coords.blockToCube(blockPosIn.getX()), Coords.blockToCube(blockPosIn.getY()), Coords.blockToCube(blockPosIn.getZ()),
                ServerWorldLightManager.Phase.POST_UPDATE,
                Util.namedRunnable(() -> {
            super.checkBlock(blockpos);
        }, () -> {
            return "checkBlock " + blockpos;
        }));
    }

    // func_215586_a
    private void schedulePhaseTask(int cubePosX, int cubePosY, int cubePosZ, ServerWorldLightManager.Phase phase, Runnable runnable) {
        this.schedulePhaseTask(cubePosX, cubePosY, cubePosZ, ((IChunkManager)this.chunkManager).getCompletedLevel(ChunkPos.asLong(cubePosX, cubePosZ)),
                phase, runnable);
    }

    // func_215600_a
    private void schedulePhaseTask(int cubePosX, int cubePosY, int cubePosZ, IntSupplier getCompletedLevel, ServerWorldLightManager.Phase p_215600_4_,
            Runnable p_215600_5_) {
        this.taskExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(() -> {
            this.field_215606_c.add(Pair.of(p_215600_4_, p_215600_5_));
            if (this.field_215606_c.size() >= this.field_215609_f) {
                this.func_215603_b();
            }

        }, CubePos.asLong(cubePosX, cubePosY, cubePosZ), getCompletedLevel));
    }

    // updateChunkStatus
    public void updateCubeStatus(CubePos cubePos) {
        this.schedulePhaseTask(cubePos.getX(), cubePos.getY(), cubePos.getZ(), () -> {
            return 0;
        }, ServerWorldLightManager.Phase.PRE_UPDATE, Util.namedRunnable(() -> {
            super.retainData(cubePos, false);
            super.enableLightSources(cubePos, false);


            for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
                super.setData(LightType.BLOCK, Coords.sectionPosByIndex(cubePos, i), (NibbleArray)null);
                super.setData(LightType.SKY, Coords.sectionPosByIndex(cubePos, i), (NibbleArray)null);
            }

            for(int j = 0; j < ICube.CUBE_SIZE; ++j) {
                super.updateSectionStatus(Coords.sectionPosByIndex(cubePos, j), true);
            }

        }, () -> {
            return "updateCubeStatus " + cubePos + " " + true;
        }));
    }

    @Override public void postConstructorSetup(CubeTaskPriorityQueueSorter sorter,
            ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor) {
        this.cubeTaskPriorityQueueSorter = sorter;
        this.taskExecutor = taskExecutor;
    }
}
