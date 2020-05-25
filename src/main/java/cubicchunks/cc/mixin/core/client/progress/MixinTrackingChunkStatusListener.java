package cubicchunks.cc.mixin.core.client.progress;

import cubicchunks.cc.chunk.ISectionStatusListener;
import cubicchunks.cc.chunk.ITrackingSectionStatusListener;
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
public abstract class MixinTrackingChunkStatusListener implements ISectionStatusListener, ITrackingSectionStatusListener {

    @Shadow private boolean tracking;

    @Shadow @Final private LoggingChunkStatusListener loggingListener;

    @Shadow @Final private int positionOffset;
    private SectionPos centerSection;
    private final Long2ObjectOpenHashMap<ChunkStatus> sectionStatuses = new Long2ObjectOpenHashMap<>();

    @Override
    public void startSections(SectionPos center) {
        if (this.tracking) {
            ((ISectionStatusListener) this.loggingListener).startSections(center);
            this.centerSection = center;
        }
    }

    @Override
    public void sectionStatusChanged(SectionPos sectionPos, @Nullable ChunkStatus newStatus) {
        if (this.tracking) {
            ((ISectionStatusListener) this.loggingListener).sectionStatusChanged(sectionPos, newStatus);
            if (newStatus == null) {
                this.sectionStatuses.remove(sectionPos.asLong());
            } else {
                this.sectionStatuses.put(sectionPos.asLong(), newStatus);
            }
        }
    }

    @Inject(method = "startTracking", at = @At("HEAD"))
    public void startTracking(CallbackInfo ci) {
        this.sectionStatuses.clear();
    }

    @Nullable @Override
    public ChunkStatus getSectionStatus(int x, int y, int z) {
        if (centerSection == null) {
            return null; // vanilla race condition, made worse by forge moving IChunkStatusListener ichunkstatuslistener = this.chunkStatusListenerFactory.create(11); earlier
        }
        return this.sectionStatuses.get(SectionPos.asLong(
                x + this.centerSection.getX() - this.positionOffset,
                y + this.centerSection.getY() - this.positionOffset,
                z + this.centerSection.getZ() - this.positionOffset));
    }
}
