package io.github.opencubicchunks.cubicchunks.mixin.core.common.progress;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.Nullable;

@Mixin(targets = "net.minecraftforge.common.DimensionManager$NoopChunkStatusListener")
public class MixinNoopChunkStatusListener implements ICubeStatusListener {

    @Override public void start(ChunkPos center) {

    }

    @Override public void statusChanged(ChunkPos chunkPosition, @Nullable ChunkStatus newStatus) {

    }

    @Override public void stop() {

    }
}
