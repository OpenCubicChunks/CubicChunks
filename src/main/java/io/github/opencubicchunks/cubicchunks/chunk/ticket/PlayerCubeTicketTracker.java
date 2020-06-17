package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
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

public class PlayerCubeTicketTracker {
    private int viewDistance;
    private int verticalViewDistance;
    private final Long2IntMap horizDistances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final Long2IntMap vertDistances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final LongSet positionsAffected = new LongOpenHashSet();
    private final ITicketManager iTicketManager;

    private HorizontalGraphGroup horizontalGraphGroup;
    private VerticalGraphGroup verticalGraphGroup;

    public PlayerCubeTicketTracker(ITicketManager iTicketManager, int i) {
        //possibly make this a constant - there is only ever one playercubeticketracker at a time, so this should be fine.
        horizontalGraphGroup = new HorizontalGraphGroup(iTicketManager, (32 / ICube.CUBE_DIAMETER) + 1, this);
        verticalGraphGroup = new VerticalGraphGroup(iTicketManager, (32 / ICube.CUBE_DIAMETER) + 1, this);
        this.iTicketManager = iTicketManager;
        this.viewDistance = 0;
        this.verticalViewDistance = 0;
        this.horizDistances.defaultReturnValue(i + 2);
        this.vertDistances.defaultReturnValue(i + 2);
    }

    void chunkLevelChanged(long cubePosIn, int oldLevel, int newLevel) {
        this.positionsAffected.add(cubePosIn);
    }

    public void setViewDistance(int viewDistanceIn, int verticalViewDistanceIn) {
        viewDistanceIn *= 2;
        CubicChunks.LOGGER.warn("Horizontal dist: {}; Vertical dist: {}", viewDistanceIn, verticalViewDistanceIn);
        for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.horizontalGraphGroup.cubesInRange.long2ByteEntrySet()) {
            long pos = entry.getLongKey();
            byte horizDistance = entry.getByteValue();
            byte vertDistance = this.horizontalGraphGroup.cubesInRange.get(pos);
            this.updateTicket(pos, horizDistance, vertDistance, this.isWithinViewDistance(horizDistance, vertDistance), horizDistance <= viewDistanceIn && vertDistance <= verticalViewDistanceIn);
        }

        this.viewDistance = viewDistanceIn;
        this.verticalViewDistance = verticalViewDistanceIn;
    }

    // func_215504_a
    private void updateTicket(long cubePosIn, int horizDistance, int vertDistance, boolean oldWithinViewDistance, boolean withinViewDistance) {
        if (oldWithinViewDistance != withinViewDistance) {
            Ticket<?> ticket = new Ticket<>(CCTicketType.CCPLAYER, ITicketManager.PLAYER_CUBE_TICKET_LEVEL, CubePos.from(cubePosIn));
            if (withinViewDistance) {
                iTicketManager.getCubePlayerTicketThrottler().enqueue(CubeTaskPriorityQueueSorter.createMsg(() ->
                        iTicketManager.executor().execute(() -> {
                            if (this.isWithinViewDistance(horizontalGraphGroup.getLevel(cubePosIn), verticalGraphGroup.getLevel(cubePosIn))) {
                                iTicketManager.registerCube(cubePosIn, ticket);
                                iTicketManager.getCubePositions().add(cubePosIn);
                            } else {
                                iTicketManager.getPlayerCubeTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                                }, cubePosIn, false));
                            }

                        }), cubePosIn, () -> horizDistance));
            } else {
                iTicketManager.getPlayerCubeTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() ->
                        iTicketManager.executor().execute(() ->
                                iTicketManager.releaseCube(cubePosIn, ticket)),
                        cubePosIn, true));
            }
        }

    }

    public void processAllUpdates() {
        horizontalGraphGroup.processAllUpdates();
        verticalGraphGroup.processAllUpdates();
        if (!this.positionsAffected.isEmpty()) {
            LongIterator itrPositions = this.positionsAffected.iterator();

            while (itrPositions.hasNext()) {
                long pos = itrPositions.nextLong();

                int oldHorizDistance = this.horizDistances.get(pos);
                int oldVertDistance = this.vertDistances.get(pos);
                int horizCurrentDistance = horizontalGraphGroup.getLevel(pos);
                int vertCurrentDistance = verticalGraphGroup.getLevel(pos);

                if (oldHorizDistance != horizCurrentDistance || oldVertDistance != vertCurrentDistance) {
                    //func_219066_a = update level
                    iTicketManager.getCubeTaskPriorityQueueSorter().onUpdateCubeLevel(CubePos.from(pos), () -> this.horizDistances.get(pos), horizCurrentDistance, (horizDistance) -> {
                        // They have the same return value and
                        if (horizCurrentDistance >= this.horizDistances.defaultReturnValue() || vertCurrentDistance >= this.vertDistances.defaultReturnValue()) {
                            this.horizDistances.remove(pos);
                            this.vertDistances.remove(pos);
                        } else {
                            this.horizDistances.put(pos, horizCurrentDistance);
                            this.vertDistances.put(pos, vertCurrentDistance);
                        }

                    });
                    this.updateTicket(pos, horizCurrentDistance, vertCurrentDistance, this.isWithinViewDistance(oldHorizDistance, oldVertDistance), this.isWithinViewDistance(horizCurrentDistance, vertCurrentDistance));
                }
            }

            this.positionsAffected.clear();
        }

    }

    private boolean isWithinViewDistance(int horizDistance, int vertDistance) {
        return horizDistance <= this.viewDistance && vertDistance <= this.verticalViewDistance;
    }

    public void updateSourceLevel(long pos, int level, boolean isDecreasing) {
        horizontalGraphGroup.updateActualSourceLevel(pos, level, isDecreasing);
        verticalGraphGroup.updateActualSourceLevel(pos, level, isDecreasing);
    }
}
