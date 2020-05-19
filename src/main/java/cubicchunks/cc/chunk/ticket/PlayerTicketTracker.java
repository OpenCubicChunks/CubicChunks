package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.chunk.graph.CCTicketType;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.Ticket;

public class PlayerTicketTracker extends PlayerCubeTracker {
    private int viewDistance;
    private final Long2IntMap distances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final LongSet positionsAffected = new LongOpenHashSet();
    private final ITicketManager iTicketManager;


    public PlayerTicketTracker(ITicketManager iTicketManager, int i) {
        super(iTicketManager, i);
        this.iTicketManager = iTicketManager;
        this.viewDistance = 0;
        this.distances.defaultReturnValue(i + 2);
    }

    protected void chunkLevelChanged(long sectionPosIn, int oldLevel, int newLevel) {
        this.positionsAffected.add(sectionPosIn);
    }

    public void setViewDistance(int viewDistanceIn) {
        for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.cubesInRange.long2ByteEntrySet()) {
            byte b0 = entry.getByteValue();
            long i = entry.getLongKey();
            this.updateTicket(i, b0, this.isWithinViewDistance(b0), b0 <= viewDistanceIn - 2);
        }

        this.viewDistance = viewDistanceIn;
    }

    private void updateTicket(long sectionPosIn, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
        if (oldWithinViewDistance != withinViewDistance) {
            Ticket<?> ticket = new Ticket<>(CCTicketType.CCPLAYER, ITicketManager.PLAYER_TICKET_LEVEL, SectionPos.from(sectionPosIn));
            if (withinViewDistance) {
                iTicketManager.getCc$playerTicketThrottler().enqueue(CubeTaskPriorityQueueSorter.createMsg(() -> {
                    iTicketManager.executor().execute(() -> {
                        if (this.isWithinViewDistance(this.getLevel(sectionPosIn))) {
                            iTicketManager.cc$register(sectionPosIn, ticket);
                            iTicketManager.getChunkPositions().add(sectionPosIn);
                        } else {
                            iTicketManager.getplayerTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                            }, sectionPosIn, false));
                        }

                    });
                }, sectionPosIn, () -> distance));
            } else {
                iTicketManager.getplayerTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                    iTicketManager.executor().execute(() -> {
                        iTicketManager.cc$release(sectionPosIn, ticket);
                    });
                }, sectionPosIn, true));
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
                    iTicketManager.getlevelUpdateListener().updateLevel(SectionPos.from(i), () -> this.distances.get(i), k, (ix) -> {
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
