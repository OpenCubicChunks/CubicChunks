package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ticket.ITicketManager;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ChunkManager.ProxyTicketManager.class)
public abstract class MixinProxyTicketManager extends MixinTicketManager {

    @Shadow ChunkManager this$0;

    @Override
    public ChunkHolder setSectionLevel(long sectionPosIn, int newLevel, @Nullable ChunkHolder holder, int oldLevel) {
        return ((IChunkManager)this$0).setSectionLevel(sectionPosIn, newLevel, holder, oldLevel);
    }

    @Override
    public boolean containsSections(long sectionPos) {
        return ((IChunkManager)this$0).getUnloadableSections().contains(sectionPos);
    }

    @Override
    public ChunkHolder getSectionHolder(long sectionPosIn) {
        return ((IChunkManager)this$0).getSectionHolder(sectionPosIn);
    }
}
