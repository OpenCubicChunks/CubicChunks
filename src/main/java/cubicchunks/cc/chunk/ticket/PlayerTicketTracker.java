package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.mixin.core.common.ticket.CCTicketManager;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.SectionDistanceGraph;
import net.minecraft.util.SortedArraySet;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;

public class PlayerTicketTracker extends PlayerCubeTracker {
    private int viewDistance;
    private final Long2IntMap distances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final LongSet positionsAffected = new LongOpenHashSet();
    private final ICCTicketManager iccTicketManager;


    public PlayerTicketTracker(ICCTicketManager iccTicketManager, int i) {
        super(iccTicketManager, i);
        this.iccTicketManager = iccTicketManager;
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
            Ticket<?> ticket = new Ticket<>(CCTicketType.CCPLAYER, ICCTicketManager.PLAYER_TICKET_LEVEL, SectionPos.from(sectionPosIn));
            if (withinViewDistance) {
                iccTicketManager.getPlayerTicketThrottler().enqueue(CubeTaskPriorityQueueSorter.createMsg(() -> {
                    iccTicketManager.executor().execute(() -> {
                        if (this.isWithinViewDistance(this.getLevel(sectionPosIn))) {
                            iccTicketManager.cc$register(sectionPosIn, ticket);
                            iccTicketManager.getChunkPositions().add(sectionPosIn);
                        } else {
                            iccTicketManager.getplayerTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                            }, sectionPosIn, false));
                        }

                    });
                }, sectionPosIn, () -> distance));
            } else {
                iccTicketManager.getplayerTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                    iccTicketManager.executor().execute(() -> {
                        iccTicketManager.cc$release(sectionPosIn, ticket);
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
                    iccTicketManager.getlevelUpdateListener().updateLevel(SectionPos.from(i), () -> this.distances.get(i), k, (ix) -> {
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
