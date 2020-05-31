package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.graph.CubeDistanceGraph;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.Ticket;

public class CubeTicketTracker extends CubeDistanceGraph {
    private final ITicketManager iTicketManager;

    public CubeTicketTracker(ITicketManager iTicketManager) {
        //TODO: change the arguments passed into super to CCCubeManager or CCColumnManager
        super(IChunkManager.MAX_CUBE_LOADED_LEVEL + 2, 16, 256);
        this.iTicketManager = iTicketManager;
    }

    @Override
    protected int getSourceLevel(long pos) {
        SortedArraySet<Ticket<?>> sortedarrayset = iTicketManager.getCubeTickets().get(pos);
        if (sortedarrayset == null) {
            return Integer.MAX_VALUE;
        } else {
            return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.getSmallest().getLevel();
        }
    }

    @Override
    protected int getLevel(long cubePosIn) {
        if (!iTicketManager.containsCubes(cubePosIn)) {
            ChunkHolder chunkholder = iTicketManager.getCubeHolder(cubePosIn);
            if (chunkholder != null) {
                return chunkholder.getChunkLevel();
            }
        }

        return IChunkManager.MAX_CUBE_LOADED_LEVEL + 1;
    }

    @Override
    protected void setLevel(long cubePosIn, int level) {
        ChunkHolder chunkholder = iTicketManager.getCubeHolder(cubePosIn);
        int i = chunkholder == null ? IChunkManager.MAX_CUBE_LOADED_LEVEL + 1 : chunkholder.getChunkLevel();
        if (i != level) {
            chunkholder = iTicketManager.setCubeLevel(cubePosIn, level, chunkholder, i);
            if (chunkholder != null) {
                iTicketManager.getCubeHolders().add(chunkholder);
            }

        }
    }

    public int update(int distance) {
        return this.processUpdates(distance);
    }
}
