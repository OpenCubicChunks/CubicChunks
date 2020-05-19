package cubicchunks.cc.chunk.ticket;

import net.minecraft.util.SectionDistanceGraph;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;

public class CubeTicketTracker extends SectionDistanceGraph  {
    private final ITicketManager iTicketManager;

    public CubeTicketTracker(ITicketManager iTicketManager) {
        //TODO: change the arguments passed into super to CCCubeManager or CCColumnManager
        super(ChunkManager.MAX_LOADED_LEVEL + 2, 16, 256);
        this.iTicketManager = iTicketManager;
    }

    @Override
    protected int getSourceLevel(long pos) {
        SortedArraySet<Ticket<?>> sortedarrayset = iTicketManager.getTickets().get(pos);
        if (sortedarrayset == null) {
            return Integer.MAX_VALUE;
        } else {
            return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.getSmallest().getLevel();
        }
    }

    @Override
    protected int getLevel(long sectionPosIn) {
        if (!((IIntrinsicCCTicketManager) iTicketManager).contains(sectionPosIn)) {
            ChunkHolder chunkholder = ((IIntrinsicCCTicketManager) iTicketManager).getChunkHolder(sectionPosIn);
            if (chunkholder != null) {
                return chunkholder.getChunkLevel();
            }
        }

        return ChunkManager.MAX_LOADED_LEVEL + 1;
    }

    @Override
    protected void setLevel(long sectionPosIn, int level) {
        ChunkHolder chunkholder = ((IIntrinsicCCTicketManager) iTicketManager).getChunkHolder(sectionPosIn);
        int i = chunkholder == null ? ChunkManager.MAX_LOADED_LEVEL + 1 : chunkholder.getChunkLevel();
        if (i != level) {
            chunkholder = ((IIntrinsicCCTicketManager) iTicketManager).setChunkLevel(sectionPosIn, level, chunkholder, i);
            if (chunkholder != null) {
                iTicketManager.getChunkHolders().add(chunkholder);
            }

        }
    }

    public int update(int distance) {
        return this.processUpdates(distance);
    }
}
