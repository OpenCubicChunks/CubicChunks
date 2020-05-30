package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.world.server.Ticket;

public class PlayerSectionTicketTracker extends PlayerSectionTracker {
    private int viewDistance;
    private final Long2IntMap distances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final LongSet positionsAffected = new LongOpenHashSet();
    private final ITicketManager iTicketManager;


    public PlayerSectionTicketTracker(ITicketManager iTicketManager, int i) {
        super(iTicketManager, i);
        this.iTicketManager = iTicketManager;
        this.viewDistance = 0;
        this.distances.defaultReturnValue(i + 2);
    }

    protected void chunkLevelChanged(long cubePosIn, int oldLevel, int newLevel) {
        this.positionsAffected.add(cubePosIn);
    }

    public void setViewDistance(int viewDistanceIn) {
        for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.sectionsInRange.long2ByteEntrySet()) {
            byte b0 = entry.getByteValue();
            long i = entry.getLongKey();
            this.updateTicket(i, b0, this.isWithinViewDistance(b0), b0 <= viewDistanceIn - 2);
        }

        this.viewDistance = viewDistanceIn;
    }

    private void updateTicket(long cubePosIn, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
        if (oldWithinViewDistance != withinViewDistance) {
            Ticket<?> ticket = new Ticket<>(CCTicketType.CCPLAYER, ITicketManager.PLAYER_CUBE_TICKET_LEVEL, CubePos.from(cubePosIn));
            if (withinViewDistance) {
                iTicketManager.getSectionPlayerTicketThrottler().enqueue(CubeTaskPriorityQueueSorter.createMsg(() -> {
                    iTicketManager.executor().execute(() -> {
                        if (this.isWithinViewDistance(this.getLevel(cubePosIn))) {
                            iTicketManager.registerSection(cubePosIn, ticket);
                            iTicketManager.getSectionPositions().add(cubePosIn);
                        } else {
                            iTicketManager.getPlayerSectionTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                            }, cubePosIn, false));
                        }

                    });
                }, cubePosIn, () -> distance));
            } else {
                iTicketManager.getPlayerSectionTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                    iTicketManager.executor().execute(() -> {
                        iTicketManager.releaseCube(cubePosIn, ticket);
                    });
                }, cubePosIn, true));
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
                    iTicketManager.getCubeTaskPriorityQueueSorter().onUpdateSectionLevel(CubePos.from(i), () -> this.distances.get(i), k, (ix) -> {
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
