package io.github.opencubicchunks.cubicchunks.mixin.core.client.progress;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.server.level.progress.CubeProgressListener;
import io.github.opencubicchunks.cubicchunks.server.level.progress.StoringCubeProgressListener;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StoringChunkProgressListener.class)
public abstract class MixinStoringChunkProgressListener implements CubeProgressListener, StoringCubeProgressListener {

    @Shadow private boolean started;

    @Shadow @Final private LoggerChunkProgressListener delegate;

    @Shadow @Final private int radius;
    @Shadow private ChunkPos spawnPos;
    private CubePos spawnCube;
    private final Long2ObjectOpenHashMap<ChunkStatus> cubeStatuses = new Long2ObjectOpenHashMap<>();

    @Override
    public void startCubes(CubePos spawn) {
        if (this.started) {
            ((CubeProgressListener) this.delegate).startCubes(spawn);
            this.spawnCube = spawn;
            this.spawnPos = spawnCube.asChunkPos();
        }
    }

    @Override
    public void onCubeStatusChange(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        if (this.started) {
            ((CubeProgressListener) this.delegate).onCubeStatusChange(cubePos, newStatus);
            if (newStatus == null) {
                this.cubeStatuses.remove(cubePos.asLong());
            } else {
                this.cubeStatuses.put(cubePos.asLong(), newStatus);
            }
        }
    }

    @Inject(method = "start", at = @At("HEAD"))
    public void startTracking(CallbackInfo ci) {
        this.cubeStatuses.clear();
    }

    @Nullable @Override
    public ChunkStatus getCubeStatus(int x, int y, int z) {
        int radiusCubes = Coords.sectionToCubeCeil(this.radius);

        if (spawnCube == null) {
            return this.cubeStatuses.get(CubePos.asLong(
                x - radiusCubes,
                y - radiusCubes,
                z - radiusCubes));
            // vanilla race condition, made worse by forge moving IChunkStatusListener ichunkstatuslistener = this.chunkStatusListenerFactory.create(11); earlier
        }
        return this.cubeStatuses.get(CubePos.asLong(
            x + this.spawnCube.getX() - radiusCubes,
            y + this.spawnCube.getY() - radiusCubes,
            z + this.spawnCube.getZ() - radiusCubes));
    }
}