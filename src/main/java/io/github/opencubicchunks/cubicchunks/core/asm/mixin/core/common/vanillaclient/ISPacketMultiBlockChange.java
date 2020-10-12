package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.vanillaclient;

import net.minecraft.network.play.server.SPacketMultiBlockChange;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SPacketMultiBlockChange.class)
public interface ISPacketMultiBlockChange {
    @Accessor void setChunkPos(ChunkPos pos);
    @Accessor void setChangedBlocks(SPacketMultiBlockChange.BlockUpdateData[] data);
}
