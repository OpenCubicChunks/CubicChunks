package io.github.opencubicchunks.cubicchunks.server.level;

import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.Ticket;
import net.minecraft.util.SortedArraySet;

public class CubeTicketTracker extends CubeTracker {
    private final CubicDistanceManager cubicDistanceManager;

    public CubeTicketTracker(CubicDistanceManager cubicDistanceManager) {
        //TODO: change the arguments passed into super to CCCubeManager or CCColumnManager
        super(CubeMap.MAX_CUBE_DISTANCE + 2, 16, 256);
        this.cubicDistanceManager = cubicDistanceManager;
    }

    @Override
    protected int getSourceLevel(long pos) {
        SortedArraySet<Ticket<?>> tickets = cubicDistanceManager.getCubeTickets().get(pos);
        if (tickets == null) {
            return Integer.MAX_VALUE;
        } else {
            return tickets.isEmpty() ? Integer.MAX_VALUE : tickets.first().getTicketLevel();
        }
    }

    @Override
    protected int getLevel(long cubePosIn) {
        if (!cubicDistanceManager.containsCubes(cubePosIn)) {
            ChunkHolder chunkholder = cubicDistanceManager.getCubeHolder(cubePosIn);
            if (chunkholder != null) {
                return chunkholder.getTicketLevel();
            }
        }
        return CubeMap.MAX_CUBE_DISTANCE + 1;
    }

    @Override
    protected void setLevel(long cubePosIn, int level) {
        ChunkHolder chunkholder = cubicDistanceManager.getCubeHolder(cubePosIn);
        int i = chunkholder == null ? CubeMap.MAX_CUBE_DISTANCE + 1 : chunkholder.getTicketLevel();
        if (i != level) {
            chunkholder = cubicDistanceManager.updateCubeScheduling(cubePosIn, level, chunkholder, i);
            if (chunkholder != null) {
                cubicDistanceManager.getCubesToUpdateFutures().add(chunkholder);
            }
        }
    }

    public int update(int distance) {
        return this.runUpdates(distance);
    }
}