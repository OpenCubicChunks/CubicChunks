package io.github.opencubicchunks.cubicchunks.chunk.ticket;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.graph.CCTicketType;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.server.Ticket;

/**
 * Keeps track of the cubes that should be loaded for players
 */
public class PlayerCubeTicketTracker {
    private int viewDistance;
    private int verticalViewDistance;

    private final Long2IntMap horizDistances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final Long2IntMap vertDistances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());

    // Set of cubes whose level changed after the last update
    private final LongSet positionsAffected = new LongOpenHashSet();

    private final ITicketManager iTicketManager;

    private final HorizontalGraphGroup horizontalGraphGroup;
    private final VerticalGraphGroup verticalGraphGroup;

    public PlayerCubeTicketTracker(ITicketManager iTicketManager, int i) {
        //possibly make this a constant - there is only ever one playercubeticketracker at a time, so this should be fine.
        horizontalGraphGroup = new HorizontalGraphGroup(iTicketManager, (32 / IBigCube.CUBE_DIAMETER) + 1, this);
        verticalGraphGroup = new VerticalGraphGroup(iTicketManager, (32 / IBigCube.CUBE_DIAMETER) + 1, this);
        this.iTicketManager = iTicketManager;
        this.viewDistance = 0;
        this.verticalViewDistance = 0;
        this.horizDistances.defaultReturnValue(i + 2);
        this.vertDistances.defaultReturnValue(i + 2);
    }

    /**
     * Called when the a cube was affected by an update
     * @param pos the packed position of the affected cube
     *
     */
    void cubeAffected(long pos) {
        this.positionsAffected.add(pos);
    }

    /**
     * Updates which cubes are to be loaded based on a new viewDistance or verticalViewDistance
     * @param viewDistance
     * @param verticalViewDistance
     */
    public void setViewDistance(int viewDistance, int verticalViewDistance) {
        verticalViewDistance = verticalViewDistance / 2;

        CubicChunks.LOGGER.warn("Horizontal dist: {}; Vertical dist: {}", viewDistance, verticalViewDistance);
        for (it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry entry : this.horizontalGraphGroup.cubesInRange.long2ByteEntrySet()) {
            long pos = entry.getLongKey();
            byte horizDistance = entry.getByteValue();
            byte vertDistance = this.verticalGraphGroup.cubesInRange.get(pos);
            this.updateTicket(pos, horizDistance, this.isWithinViewDistance(horizDistance, vertDistance), horizDistance <= viewDistance - 2 && vertDistance <= verticalViewDistance - 2);
        }

        this.viewDistance = viewDistance;
        this.verticalViewDistance = verticalViewDistance;
    }

    /**
     * Updates a ticket for a cube based on whether it entered or exited the view distances
     * @param pos the cube's position
     * @param horizDistance the cube's horizontal distance
     * @param oldWithinViewDistance whether it used to be in the view distance
     * @param withinViewDistance whether it is now in the view distance
     */
    private void updateTicket(long pos, int horizDistance, boolean oldWithinViewDistance, boolean withinViewDistance) {
        if (oldWithinViewDistance != withinViewDistance) {
            Ticket<?> ticket = new Ticket<>(CCTicketType.CCPLAYER, ITicketManager.PLAYER_CUBE_TICKET_LEVEL, CubePos.from(pos));
            if (withinViewDistance) {
                iTicketManager.getCubePlayerTicketThrottler().enqueue(CubeTaskPriorityQueueSorter.createMsg(() ->
                        iTicketManager.executor().execute(() -> {
                            if (this.isWithinViewDistance(horizontalGraphGroup.getLevel(pos), verticalGraphGroup.getLevel(pos))) {
                                iTicketManager.registerCube(pos, ticket);
                                iTicketManager.getCubePositions().add(pos);
                            } else {
                                iTicketManager.getPlayerCubeTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                                }, pos, false));
                            }

                        }), pos, () -> horizDistance));
            } else {
                iTicketManager.getPlayerCubeTicketThrottlerSorter().enqueue(CubeTaskPriorityQueueSorter.createSorterMsg(() ->
                                iTicketManager.executor().execute(() ->
                                        iTicketManager.releaseCube(pos, ticket)),
                        pos, true));
            }
        }

    }

    /**
     * Deal with all of the updates to the graphs
     */
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
                    iTicketManager.getCubeTaskPriorityQueueSorter().onUpdateCubeLevel(CubePos.from(pos), () -> this.horizDistances.get(pos), horizCurrentDistance+verticalViewDistance, (horizDistance) -> {
                        // They have the same return value and
                        if (horizCurrentDistance >= this.horizDistances.defaultReturnValue() || vertCurrentDistance >= this.vertDistances.defaultReturnValue()) {
                            this.horizDistances.remove(pos);
                            this.vertDistances.remove(pos);
                        } else {
                            this.horizDistances.put(pos, horizCurrentDistance);
                            this.vertDistances.put(pos, vertCurrentDistance);
                        }

                    });
                    this.updateTicket(pos, horizCurrentDistance, this.isWithinViewDistance(oldHorizDistance, oldVertDistance), this.isWithinViewDistance(horizCurrentDistance, vertCurrentDistance));
                }
            }

            this.positionsAffected.clear();
        }

    }

    /**
     * Determines whether a given set of distances are within the view distance bounds
     * @param horizDistance the horizontal distance
     * @param vertDistance the vertical distance
     * @return Whether the distances are within the view distance bounds
     */
    private boolean isWithinViewDistance(int horizDistance, int vertDistance) {
        return horizDistance <= this.viewDistance - 2 && vertDistance <= this.verticalViewDistance - 2;
    }

    /**
     * Notify the graphs of a change of source level of a cube
     * @param pos the cube's position
     * @param level the cube's level
     * @param isDecreasing whether the change is decreasing
     */
    public void updateSourceLevel(long pos, int level, boolean isDecreasing) {
        horizontalGraphGroup.updateActualSourceLevel(pos, level, isDecreasing);
        verticalGraphGroup.updateActualSourceLevel(pos, level, isDecreasing);
    }
}
