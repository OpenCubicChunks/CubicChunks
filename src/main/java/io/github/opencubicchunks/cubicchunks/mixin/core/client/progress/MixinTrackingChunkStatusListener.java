package io.github.opencubicchunks.cubicchunks.mixin.core.client.progress;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.ITrackingCubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import net.minecraft.world.chunk.listener.TrackingChunkStatusListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(TrackingChunkStatusListener.class)
public abstract class MixinTrackingChunkStatusListener implements ICubeStatusListener, ITrackingCubeStatusListener {

    @Shadow private boolean tracking;

    @Shadow @Final private LoggingChunkStatusListener loggingListener;

    @Shadow @Final private int positionOffset;
    @Shadow private ChunkPos center;
    private CubePos centerCube;
    private final Long2ObjectOpenHashMap<ChunkStatus> cubeStatuses = new Long2ObjectOpenHashMap<>();

    @Override
    public void startCubes(CubePos center) {
        if (this.tracking) {
            ((ICubeStatusListener) this.loggingListener).startCubes(center);
            this.centerCube = center;
            this.center = centerCube.asChunkPos();
        }
    }

    @Override
    public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        if (this.tracking) {
            ((ICubeStatusListener) this.loggingListener).cubeStatusChanged(cubePos, newStatus);
            if (newStatus == null) {
                this.cubeStatuses.remove(cubePos.asLong());
            } else {
                this.cubeStatuses.put(cubePos.asLong(), newStatus);
            }
        }
    }

    @Inject(method = "startTracking", at = @At("HEAD"))
    public void startTracking(CallbackInfo ci) {
        this.cubeStatuses.clear();
    }

    @Nullable @Override
    public ChunkStatus getCubeStatus(int x, int y, int z) {
        if (centerCube == null) {
            return null; // vanilla race condition, made worse by forge moving IChunkStatusListener ichunkstatuslistener = this.chunkStatusListenerFactory.create(11); earlier
        }
        int offset = Coords.sectionToCubeCeil(this.positionOffset);
        return this.cubeStatuses.get(CubePos.asLong(
                x + this.centerCube.getX() - offset,
                y + this.centerCube.getY() - offset,
                z + this.centerCube.getZ() - offset));
    }
}
