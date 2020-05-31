package cubicchunks.cc.mixin.core.common.progress;

import cubicchunks.cc.chunk.ICubeStatusListener;
import cubicchunks.cc.chunk.util.CubePos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(LoggingChunkStatusListener.class)
public abstract class MixinLoggingChunkStatusListener implements ICubeStatusListener {

    @Shadow public abstract void statusChanged(ChunkPos chunkPosition, @Nullable ChunkStatus newStatus);

    @Override public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        //this.statusChanged(chunkPosition.asChunkPos(), newStatus);
    }
}
