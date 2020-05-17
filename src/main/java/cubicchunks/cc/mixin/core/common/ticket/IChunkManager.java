package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkManager.class)
public interface IChunkManager {

    @Invoker("func_219220_a")
    ChunkHolder invokefunc_219220_a(long chunkPosIn);

}
