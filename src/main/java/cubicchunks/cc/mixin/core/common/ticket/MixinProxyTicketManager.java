package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.IChunkManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ChunkManager.ProxyTicketManager.class)
public abstract class MixinProxyTicketManager extends MixinTicketManager {

    @Shadow ChunkManager this$0;

    @Override
    public ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        return ((IChunkManager)this$0).setCubeLevel(sectionPosIn, newLevel, holder, oldLevel);
    }

    @Override
    public boolean containsSections(long sectionPos) {
        return ((IChunkManager)this$0).getUnloadableCubes().contains(sectionPos);
    }

    @Override
    public ChunkHolder getCubeHolder(long sectionPosIn) {
        return ((IChunkManager)this$0).getCubeHolder(sectionPosIn);
    }
}
