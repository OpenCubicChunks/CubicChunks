package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkHolder.class)
public interface IMixinChunkHolder {

    @Invoker("processUpdates")
    void processUpdates(ChunkManager chunkManagerIn);

    @Invoker("func_219220_a") ChunkHolder func_219220_a(long chunkPosIn);
}
