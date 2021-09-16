package io.github.opencubicchunks.cubicchunks.server.level;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.level.ServerPlayer;

public class HorizontalGraphGroup extends CubeTracker {
    public final Long2ByteMap cubesInRange = new Long2ByteOpenHashMap();
    protected final int range;

    // TODO: remove this field
    private final CubicDistanceManager cubicDistanceManager;
    private final CubicPlayerTicketTracker ticketTracker;
    private final Vertical vertical;

    public HorizontalGraphGroup(CubicDistanceManager cubicDistanceManager, int i, CubicPlayerTicketTracker ticketTracker) {
        super(i + 2, 16, 256, 1, 0, 1);
        this.ticketTracker = ticketTracker;
        this.vertical = new Vertical(cubicDistanceManager, i);
        this.range = i;
        this.cubesInRange.defaultReturnValue((byte) (i + 2));
        this.cubicDistanceManager = cubicDistanceManager;
    }

    @Override
    protected int getLevel(long cubePosIn) {
        return this.cubesInRange.get(cubePosIn);
    }

    @Override
    protected void setLevel(long cubePosIn, int level) {
        byte b0;
        if (level > this.range) {
            b0 = this.cubesInRange.remove(cubePosIn);
        } else {
            b0 = this.cubesInRange.put(cubePosIn, (byte) level);
        }

        this.chunkLevelChanged(cubePosIn, b0, level);
    }

    protected void chunkLevelChanged(long cubePosIn, int oldLevel, int newLevel) {
        ticketTracker.cubeAffected(cubePosIn);
    }

    @Override
    protected int getSourceLevel(long pos) {
        return this.vertical.cubesInRange.containsKey(pos) ? 0 : Integer.MAX_VALUE;
    }

    public void processAllUpdates() {
        this.vertical.processAllUpdates();
        this.runUpdates(Integer.MAX_VALUE);
    }

    public void updateActualSourceLevel(long pos, int level, boolean isDecreasing) {
        vertical.updateSourceLevel(pos, level, isDecreasing);
    }

    private class Vertical extends CubeTracker {
        private final int range;
        private final Long2ByteMap cubesInRange = new Long2ByteOpenHashMap();
        private final CubicDistanceManager cubicDistanceManager;
        private final HorizontalGraphGroup superior;

        Vertical(CubicDistanceManager cubicDistanceManager, int range) {
            super(range + 2, 16, 256, 0, 1, 0);
            this.range = range;
            this.cubesInRange.defaultReturnValue((byte) (range + 2));
            this.cubicDistanceManager = cubicDistanceManager;
            this.superior = HorizontalGraphGroup.this;

        }

        @Override
        protected int getLevel(long cubePosIn) {
            return this.cubesInRange.get(cubePosIn);
        }

        @Override
        protected void setLevel(long cubePosIn, int level) {
            byte b0;
            if (level > this.range) {
                b0 = this.cubesInRange.remove(cubePosIn);
            } else {
                b0 = this.cubesInRange.put(cubePosIn, (byte) level);
            }

            this.chunkLevelChanged(cubePosIn, b0, level);
        }

        protected void chunkLevelChanged(long pos, int oldLevel, int newLevel) {
            boolean contained = this.cubesInRange.containsKey(pos);
            int actualNewLevel = contained ? 0 : Integer.MAX_VALUE;
            superior.updateSourceLevel(pos, actualNewLevel, contained);
        }

        @Override
        protected int getSourceLevel(long pos) {
            return this.hasPlayerInChunk(pos) ? 0 : Integer.MAX_VALUE;
        }

        private boolean hasPlayerInChunk(long cubePosIn) {
            ObjectSet<ServerPlayer> objectset = cubicDistanceManager.getPlayersPerCube().get(cubePosIn);
            return objectset != null && !objectset.isEmpty();
        }

        public void processAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }
    }
}