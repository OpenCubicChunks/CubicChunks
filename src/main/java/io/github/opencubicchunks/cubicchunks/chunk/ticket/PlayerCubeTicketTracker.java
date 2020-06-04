package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.server.Ticket;

public class PlayerCubeTicketTracker extends PlayerCubeTracker {
    private int viewDistance;
    private final Long2IntMap distances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final LongSet positionsAffected = new LongOpenHashSet();
    private final ITicketManager iTicketManager;


    public PlayerCubeTicketTracker(ITicketManager iTicketManager, int i) {
        //possibly make this a constant - there is only ever one playercubeticketracker at a time, so this should be fine.
        super(iTicketManager, (32 / ICube.CUBE_DIAMETER) + 1);
        this.iTicketManager = iTicketManager;
        this.viewDistance = 0;
        this.distances.defaultReturnValue(i + 2);
    }

    protected void chunkLevelChanged(long cubePosIn, int oldLevel, int newLevel) {
        this.positionsAffected.add(cubePosIn);
    }

    public void setViewDistance(int viewDistanceIn) {
        for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.cubesInRange.long2ByteEntrySet()) {
            byte b0 = entry.getByteValue();
            long i = entry.getLongKey();
            this.updateTicket(i, b0, this.isWithinViewDistance(b0), b0 <= viewDistanceIn - 2);
        }

        this.viewDistance = viewDistanceIn;
    }

    // func_215504_a
    private void updateTicket(long cubePosIn, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
        if (oldWithinViewDistance != withinViewDistance) {
            Ticket<?> ticket = new Ticket<>(CCTicketType.CCPLAYER, ITicketManager.PLAYER_CUBE_TICKET_LEVEL, CubePos.from(cubePosIn));
            if (withinViewDistance) {
                iTicketManager.getCubePlayerTicketThrottler().enqueue(CubeTaskPriorityQueueSorter.createMsg(() ->
                        iTicketManager.executor().execute(() -> {
                            if (this.isWithinViewDistance(this.getLevel(cubePosIn))) {
                                iTicketManager.registerCube(cubePosIn, ticket);
                                iTicketManager.getCubePositions().add(cubePosIn);
                            } else {
                                iTicketManager.getPlayerCubeTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                                }, cubePosIn, false));
                            }

                        }), cubePosIn, () -> distance));
            } else {
                iTicketManager.getPlayerCubeTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() ->
                        iTicketManager.executor().execute(() ->
                                iTicketManager.releaseCube(cubePosIn, ticket)),
                        cubePosIn, true));
            }
        }

    }

    public void processAllUpdates() {
        super.processAllUpdates();
        if (!this.positionsAffected.isEmpty()) {
            LongIterator longiterator = this.positionsAffected.iterator();

            while (longiterator.hasNext()) {
                long i = longiterator.nextLong();
                int j = this.distances.get(i);
                int k = this.getLevel(i);
                if (j != k) {
                    //func_219066_a = update level
                    iTicketManager.getCubeTaskPriorityQueueSorter().onUpdateCubeLevel(CubePos.from(i), () -> this.distances.get(i), k, (ix) -> {
                        if (ix >= this.distances.defaultReturnValue()) {
                            this.distances.remove(i);
                        } else {
                            this.distances.put(i, ix);
                        }

                    });
                    this.updateTicket(i, k, this.isWithinViewDistance(j), this.isWithinViewDistance(k));
                }
            }

            this.positionsAffected.clear();
        }

    }

    private boolean isWithinViewDistance(int p_215505_1_) {
        return p_215505_1_ <= this.viewDistance - 2;
    }
}
