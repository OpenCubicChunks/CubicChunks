package io.github.opencubicchunks.cubicchunks.server.level;

import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.server.level.ServerPlayer;

public class FixedPlayerDistanceCubeTracker extends CubeTracker {
    public final Long2ByteMap cubesInRange = new Long2ByteOpenHashMap();
    protected final int range;

    private final CubicDistanceManager cubicDistanceManager;

    public FixedPlayerDistanceCubeTracker(CubicDistanceManager cubicDistanceManager, int i) {
        super(i + 2, 16, 256);
        this.range = i;
        this.cubesInRange.defaultReturnValue((byte) (i + 2));
        this.cubicDistanceManager = cubicDistanceManager;
    }

    protected int getLevel(long cubePosIn) {
        return this.cubesInRange.get(cubePosIn);
    }

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
    }

    protected int getSourceLevel(long pos) {
        return this.hasPlayerInChunk(pos) ? 0 : Integer.MAX_VALUE;
    }

    private boolean hasPlayerInChunk(long cubePosIn) {
        ObjectSet<ServerPlayer> cubePlayers = cubicDistanceManager.getPlayersPerCube().get(cubePosIn);
        return cubePlayers != null && !cubePlayers.isEmpty();
    }

    public void processAllUpdates() {
        this.runUpdates(Integer.MAX_VALUE);
    }
}