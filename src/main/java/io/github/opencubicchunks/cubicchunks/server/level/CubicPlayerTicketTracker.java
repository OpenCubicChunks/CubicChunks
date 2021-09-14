package io.github.opencubicchunks.cubicchunks.server.level;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.TicketAccess;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.server.level.Ticket;

public class CubicPlayerTicketTracker extends FixedPlayerDistanceCubeTracker {
    private int horizontalViewDistance;
    private int verticalViewDistance;

    private final Long2IntMap horizDistances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final Long2IntMap vertDistances = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final LongSet positionsAffected = new LongOpenHashSet();
    private final CubicDistanceManager cubicDistanceManager;
    private final HorizontalGraphGroup horizontalGraphGroup;
    private final VerticalGraphGroup verticalGraphGroup;


    public CubicPlayerTicketTracker(CubicDistanceManager cubicDistanceManager, int i) {
        //possibly make this a constant - there is only ever one playercubeticketracker at a time, so this should be fine.
        super(cubicDistanceManager, (32 / CubeAccess.DIAMETER_IN_SECTIONS) + 1);
        horizontalGraphGroup = new HorizontalGraphGroup(cubicDistanceManager, (32 / CubeAccess.DIAMETER_IN_SECTIONS) + 1, this);
        verticalGraphGroup = new VerticalGraphGroup(cubicDistanceManager, (32 / CubeAccess.DIAMETER_IN_SECTIONS) + 1, this);
        this.cubicDistanceManager = cubicDistanceManager;
        this.horizontalViewDistance = 0;
        this.verticalViewDistance = 0;
        this.horizDistances.defaultReturnValue(i + 2);
        this.vertDistances.defaultReturnValue(i + 2);
    }

    /**
     * Called when the a cube was affected by an update
     *
     * @param pos the packed position of the affected cube
     */
    void cubeAffected(long pos) {
        this.positionsAffected.add(pos);
    }

    /**
     * Updates which cubes are to be loaded based on a new hViewDistance or vViewDistance
     */
    public void updateCubeViewDistance(int hViewDistance, int vViewDistance) {
        CubicChunks.LOGGER.info("Horizontal dist(Cubes): {}; Vertical dist(Cubes): {}", hViewDistance, vViewDistance);
        for (Long2ByteMap.Entry entry : this.horizontalGraphGroup.cubesInRange.long2ByteEntrySet()) {
            long pos = entry.getLongKey();
            byte horizDistance = entry.getByteValue();
            byte vertDistance = this.verticalGraphGroup.cubesInRange.get(pos);
            this.updateTicket(pos, horizDistance, this.isWithinViewDistance(horizDistance, vertDistance), horizDistance <= hViewDistance - 2 && vertDistance <= vViewDistance - 2);
        }

        this.horizontalViewDistance = hViewDistance;
        this.verticalViewDistance = vViewDistance;
    }

    /**
     * Updates a ticket for a cube based on whether it entered or exited the view distances
     *
     * @param pos the cube's position
     * @param distance the cube's distance
     * @param oldWithinViewDistance whether it used to be in the view distance
     * @param withinViewDistance whether it is now in the view distance
     */
    private void updateTicket(long pos, int distance, boolean oldWithinViewDistance, boolean withinViewDistance) {
        if (oldWithinViewDistance != withinViewDistance) {
            Ticket<?> ticket = TicketAccess.createNew(CubicTicketType.CCPLAYER, CubicDistanceManager.PLAYER_CUBE_TICKET_LEVEL, CubePos.from(pos));
            if (withinViewDistance) {
                cubicDistanceManager.getCubeTicketThrottlerInput().tell(CubeTaskPriorityQueueSorter.createMsg(() ->
                    cubicDistanceManager.getMainThreadExecutor().execute(() -> {
                        if (this.isWithinViewDistance(horizontalGraphGroup.getLevel(pos), verticalGraphGroup.getLevel(pos))) {
                            cubicDistanceManager.addCubeTicket(pos, ticket);
                            cubicDistanceManager.getCubeTicketsToRelease().add(pos);
                        } else {
                            cubicDistanceManager.getCubeTicketThrottlerReleaser().tell(CubeTaskPriorityQueueSorter.createSorterMsg(() -> {
                            }, pos, false));
                        }

                    }), pos, () -> distance));
            } else {
                cubicDistanceManager.getCubeTicketThrottlerReleaser().tell(CubeTaskPriorityQueueSorter.createSorterMsg(() ->
                        cubicDistanceManager.getMainThreadExecutor().execute(() ->
                            cubicDistanceManager.removeCubeTicket(pos, ticket)),
                    pos, true));
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
                    cubicDistanceManager.getCubeTicketThrottler()
                        .onCubeLevelChange(CubePos.from(pos), () -> Math.max(this.horizDistances.get(pos), this.vertDistances.get(pos)), Math.max(horizCurrentDistance, vertCurrentDistance),
                            (distance) -> {
                                if (horizCurrentDistance >= this.horizDistances.defaultReturnValue() || vertCurrentDistance >= this.vertDistances.defaultReturnValue()) {
                                    this.horizDistances.remove(pos);
                                    this.vertDistances.remove(pos);
                                } else {
                                    this.horizDistances.put(pos, horizCurrentDistance);
                                    this.vertDistances.put(pos, vertCurrentDistance);
                                }
                            });
                    this.updateTicket(pos, Math.max(horizCurrentDistance, vertCurrentDistance),
                        this.isWithinViewDistance(oldHorizDistance, oldVertDistance),
                        this.isWithinViewDistance(horizCurrentDistance, vertCurrentDistance));
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
        return horizDistance <= this.horizontalViewDistance - 2 && vertDistance <= this.verticalViewDistance - 2;
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