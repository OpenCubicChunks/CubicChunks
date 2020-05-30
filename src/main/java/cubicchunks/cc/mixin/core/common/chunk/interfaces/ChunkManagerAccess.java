package cubicchunks.cc.mixin.core.common.chunk.interfaces;

import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChunkManager.class)
public interface ChunkManagerAccess {

    @Invoker("func_219220_a")
    ChunkHolder chunkHold(long chunkPosIn);

    @Invoker("refreshOffThreadCache")
    boolean refreshOffThreadCacheSection();

    @Accessor int getViewDistance();
}
