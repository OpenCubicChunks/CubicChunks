package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CubeDistanceGraph;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;

public class CubeTicketTracker extends CubeDistanceGraph {
    private final ITicketManager iTicketManager;

    public CubeTicketTracker(ITicketManager iTicketManager) {
        //TODO: change the arguments passed into super to CCCubeManager or CCColumnManager
        super(IChunkManager.MAX_CUBE_DISTANCE + 2, 16, 256);
        this.iTicketManager = iTicketManager;
    }

    @Override
    protected int getSourceLevel(long pos) {
        SortedArraySet<Ticket<?>> sortedarrayset = iTicketManager.getCubeTickets().get(pos);
        if (sortedarrayset == null) {
            return Integer.MAX_VALUE;
        } else {
            return sortedarrayset.isEmpty() ? Integer.MAX_VALUE : sortedarrayset.first().getTicketLevel();
        }
    }

    @Override
    protected int getLevel(long cubePosIn) {
        if (!iTicketManager.containsCubes(cubePosIn)) {
            ChunkHolder chunkholder = iTicketManager.getCubeHolder(cubePosIn);
            if (chunkholder != null) {
                return chunkholder.getTicketLevel();
            }
        }

        return IChunkManager.MAX_CUBE_DISTANCE + 1;
    }

    @Override
    protected void setLevel(long cubePosIn, int level) {
        ChunkHolder chunkholder = iTicketManager.getCubeHolder(cubePosIn);
        int i = chunkholder == null ? IChunkManager.MAX_CUBE_DISTANCE + 1 : chunkholder.getTicketLevel();
        if (i != level) {
            chunkholder = iTicketManager.updateCubeScheduling(cubePosIn, level, chunkholder, i);
            if (chunkholder != null) {
                iTicketManager.getCubesToUpdateFutures().add(chunkholder);
            }

        }
    }

    public int update(int distance) {
        return this.runUpdates(distance);
    }
}