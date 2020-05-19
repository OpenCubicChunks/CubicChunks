package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.mixin.core.common.ticket.CCTicketManager;
import net.minecraft.util.SectionDistanceGraph;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;

public class CubeTicketTracker extends SectionDistanceGraph  {
    private final ICCTicketManager iccTicketManager;

    public CubeTicketTracker(ICCTicketManager iccTicketManager) {
        //TODO: change the arguments passed into super to CCCubeManager or CCColumnManager
        super(ChunkManager.MAX_LOADED_LEVEL + 2, 16, 256);
        this.iccTicketManager = iccTicketManager;
    }

    @Override
    protected int getSourceLevel(long pos) {
        SortedArraySet<Ticket<?>> sortedarrayset = iccTicketManager.getTickets().get(pos);
        if (sortedarrayset == null) {
            return Integer.MAX_VALUE;
        } else {
            return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.getSmallest().getLevel();
        }
    }

    @Override
    protected int getLevel(long sectionPosIn) {
        if (!iccTicketManager.contains(sectionPosIn)) {
            ChunkHolder chunkholder = iccTicketManager.getChunkHolder(sectionPosIn);
            if (chunkholder != null) {
                return chunkholder.getChunkLevel();
            }
        }

        return ChunkManager.MAX_LOADED_LEVEL + 1;
    }

    @Override
    protected void setLevel(long sectionPosIn, int level) {
        ChunkHolder chunkholder = iccTicketManager.getChunkHolder(sectionPosIn);
        int i = chunkholder == null ? ChunkManager.MAX_LOADED_LEVEL + 1 : chunkholder.getChunkLevel();
        if (i != level) {
            chunkholder = iccTicketManager.setChunkLevel(sectionPosIn, level, chunkholder, i);
            if (chunkholder != null) {
                iccTicketManager.chunkHolders.add(chunkholder);
            }

        }
    }

    public int update(int distance) {
        return this.processUpdates(distance);
    }
}
