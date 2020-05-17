package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkHolder.class)
public interface IMixinChunkHolder {

    @Invoker("processUpdates")
    void invokeprocessUpdates(ChunkManager chunkManagerIn);

//    @Invoker("invokefunc_219220_a") ChunkHolder invokefunc_219220_a(long chunkPosIn);
}
