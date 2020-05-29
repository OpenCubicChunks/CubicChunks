package cubicchunks.cc.mixin.core.client.progress;

import cubicchunks.cc.chunk.ICubeStatusListener;
import cubicchunks.cc.chunk.ITrackingCubeStatusListener;
import cubicchunks.cc.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.SectionPos;
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
    private SectionPos centerSection;
    private final Long2ObjectOpenHashMap<ChunkStatus> cubeStatuses = new Long2ObjectOpenHashMap<>();

    @Override
    public void startSections(SectionPos center) {
        if (this.tracking) {
            ((ICubeStatusListener) this.loggingListener).startCubes(center);
            this.centerSection = center;
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
        if (centerSection == null) {
            return null; // vanilla race condition, made worse by forge moving IChunkStatusListener ichunkstatuslistener = this.chunkStatusListenerFactory.create(11); earlier
        }
        return this.cubeStatuses.get(SectionPos.asLong(
                x + this.centerSection.getX() - this.positionOffset,
                y + this.centerSection.getY() - this.positionOffset,
                z + this.centerSection.getZ() - this.positionOffset));
    }
}
