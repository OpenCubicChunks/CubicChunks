package io.github.opencubicchunks.cubicchunks.world.level;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.Queues;
import io.github.opencubicchunks.cubicchunks.world.CubicServerTickList;
import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ServerTickListAccess;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.chunk.entity.ChunkEntityStateEventHandler;
import it.unimi.dsi.fastutil.longs.Long2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerTickList;
import net.minecraft.world.level.TickNextTickData;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.jetbrains.annotations.NotNull;

public class CubicFastServerTickList<T> extends ServerTickList<T> implements ChunkEntityStateEventHandler, CubicServerTickList<T> {
    private final Function<T, ResourceLocation> toId;

    private final ServerLevel level;
    private final Consumer<TickNextTickData<T>> ticker;

    private final Long2ObjectOpenHashMap<TickListSet<T>> tickNextTickCubeMap = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectAVLTreeMap<TickListBucket<T>> tickNextTickSorted = new Long2ObjectAVLTreeMap<>();

    private final Queue<TickNextTickData<T>> currentlyTicking = Queues.newArrayDeque();

    public CubicFastServerTickList(ServerLevel level, Predicate<T> predicate,
                                   Function<T, ResourceLocation> toId,
                                   Consumer<TickNextTickData<T>> ticker) {
        super(level, predicate, toId, ticker);
        this.toId = toId;
        this.level = level;
        this.ticker = ticker;
    }

    @Override public void onCubeEntitiesLoad(CubePos pos) {
        TickListSet<T> ticks = cubeTickListFor(pos.asLong());
        assert !ticks.isFullyTicking();
        ticks.entitiesLoaded = true;
        if (ticks.isFullyTicking()) {
            ticks.set.forEach(this::addToSortedList);
        }
    }

    @Override public void onCubeEntitiesUnload(CubePos pos) {
        long posLong = pos.asLong();
        TickListSet<T> ticks = cubeTickListFor(posLong);
        boolean prevTicking = ticks.isFullyTicking();
        ticks.entitiesLoaded = false;
        if (prevTicking) {
            ticks.set.forEach(this::removeFromSortedList);
        }
        if (ticks.isUnloadable()) {
            tickNextTickCubeMap.remove(posLong);
        }
    }

    public void onCubeStartTicking(CubePos pos) {
        TickListSet<T> ticks = cubeTickListFor(pos.asLong());
        assert !ticks.isFullyTicking();
        ticks.tickable = true;
        if (ticks.isFullyTicking()) {
            ticks.set.forEach(this::addToSortedList);
        }
    }

    public void onCubeStopTicking(CubePos pos) {
        long posLong = pos.asLong();
        TickListSet<T> ticks = cubeTickListFor(posLong);
        boolean prevTicking = ticks.isFullyTicking();
        ticks.tickable = false;
        if (prevTicking) {
            ticks.set.forEach(this::removeFromSortedList);
        }
        if (ticks.isUnloadable()) {
            tickNextTickCubeMap.remove(posLong);
        }
    }

    @NotNull private CubicFastServerTickList.TickListSet<T> cubeTickListFor(long l) {
        return tickNextTickCubeMap.computeIfAbsent(l, x -> new TickListSet<>());
    }

    private void addToSortedList(TickNextTickData<T> t) {
        tickNextTickSorted.computeIfAbsent(t.triggerTick, TickListBucket::new).add(t);
    }

    private void removeFromSortedList(TickNextTickData<T> tick) {
        tickNextTickSorted.get(tick.triggerTick).remove(tick);
    }

    @Override
    public void tick() {
        this.level.getProfiler().push("cleaning");
        selectTicks(65536);
        this.level.getProfiler().popPush("ticking");
        runTicks();
        this.level.getProfiler().pop();
        this.currentlyTicking.clear();
    }

    private void runTicks() {
        TickNextTickData<T> tickData;
        while ((tickData = this.currentlyTicking.poll()) != null) {
            try {
                this.ticker.accept(tickData);
            } catch (Throwable ex) {
                CrashReport report = CrashReport.forThrowable(ex, "Exception while ticking");
                CrashReportCategory category = report.addCategory("Block being ticked");
                CrashReportCategory.populateBlockDetails(category, this.level, tickData.pos, null);
                throw new ReportedException(report);
            }
        }
    }

    private void selectTicks(int count) {
        Long2ObjectSortedMap<TickListBucket<T>> bucketsToTick = this.tickNextTickSorted.subMap(Long.MIN_VALUE, this.level.getGameTime());

        long lastCube = CubePos.asLong(0, 0, 0);
        TickListSet<T> lastTicks = tickNextTickCubeMap.get(lastCube);

        var iterator = bucketsToTick.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            for (ArrayDeque<TickNextTickData<T>> queue : entry.getValue().byPriority) {
                while (!queue.isEmpty()) {
                    if (count <= 0) {
                        return;
                    }
                    count--;

                    TickNextTickData<T> tickData = queue.pop();
                    long cubeLong = CubePos.asLong(tickData.pos);
                    if (lastCube != cubeLong) {
                        lastCube = cubeLong;
                        lastTicks = tickNextTickCubeMap.get(cubeLong);
                    }
                    lastTicks.set.remove(tickData);
                    this.currentlyTicking.add(tickData);
                }
            }
            iterator.remove();
        }
    }

    @Override
    public boolean willTickThisTick(BlockPos pos, T object) {
        return currentlyTicking.contains(new TickNextTickData<>(pos, object));
    }

    @Override
    public List<TickNextTickData<T>> fetchTicksInChunk(ChunkPos pos, boolean updateState, boolean getStaleTicks) {
        if (pos instanceof ImposterChunkPos) {
            return fetchTicksInCube(((ImposterChunkPos) pos).toCubePos(), updateState, getStaleTicks);
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override public List<TickNextTickData<T>> fetchTicksInCube(CubePos pos, boolean updateState, boolean getStaleTicks) {
        // currentlyTicking and alreadyTicked are not actually necessary here since this is never called while ticking
        TickListSet<T> ticks = tickNextTickCubeMap.get(pos.asLong());
        if (ticks == null) {
            return new ArrayList<>();
        }
        ArrayList<TickNextTickData<T>> output = new ArrayList<>(ticks.set);
        if (updateState) {
            ticks.set.clear(); // Vanilla calls this from packTicks, so don't remove from cube map
            if (ticks.isFullyTicking()) {
                output.forEach(this::removeFromSortedList);
            }
        }
        return output;
    }

    @Override
    public List<TickNextTickData<T>> fetchTicksInArea(BoundingBox bounds, boolean updateState, boolean getStaleTicks) {
        int minX = Coords.blockToCube(bounds.minX());
        int minY = Coords.blockToCube(bounds.minY());
        int minZ = Coords.blockToCube(bounds.minZ());
        // max is exclusive in vanilla
        int maxY = Coords.blockToCube(bounds.maxX() - 1);
        int maxX = Coords.blockToCube(bounds.maxY() - 1);
        int maxZ = Coords.blockToCube(bounds.maxZ() - 1);

        int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        ArrayList<TickNextTickData<T>> output = new ArrayList<>();

        if (tickNextTickCubeMap.size() < volume) {
            tickNextTickCubeMap.long2ObjectEntrySet().fastForEach(entry -> {
                if (CubePos.isLongInsideInclusive(entry.getLongKey(), minX, minY, minZ, maxX, maxY, maxZ)) {
                    fetchMatchingTicksInCube(bounds, updateState, entry.getValue(), output);
                }
            });
        } else {
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        TickListSet<T> ticks = tickNextTickCubeMap.get(CubePos.asLong(x, y, z));
                        if (ticks != null) {
                            fetchMatchingTicksInCube(bounds, updateState, ticks, output);
                        }
                    }
                }
            }
        }
        return output;
    }

    private void fetchMatchingTicksInCube(BoundingBox bounds, boolean updateState, TickListSet<T> ticks, List<TickNextTickData<T>> out) {
        boolean fullyTicking = ticks.isFullyTicking();
        CubePos cubePos = null;
        Iterator<TickNextTickData<T>> it = ticks.set.iterator();
        while (it.hasNext()) {
            TickNextTickData<T> tick = it.next();
            BlockPos pos = tick.pos;
            if (isOutside(bounds, pos)) {
                continue;
            }
            out.add(tick);
            if (updateState) {
                if (cubePos == null) {
                    cubePos = CubePos.from(pos);
                }
                it.remove();
                if (fullyTicking) {
                    removeFromSortedList(tick);
                }
            }
        }
    }

    private static boolean isOutside(BoundingBox bounds, BlockPos pos) {
        return pos.getX() < bounds.minX() || pos.getY() < bounds.minY() || pos.getZ() < bounds.minZ()
            || pos.getX() >= bounds.maxX() || pos.getY() >= bounds.maxY() || pos.getZ() >= bounds.maxZ();
    }

    // vanilla save() and copy() is fine

    @Override public ListTag save(CubePos cubePos) {
        List<TickNextTickData<T>> list = this.fetchTicksInCube(cubePos, false, true);
        return ServerTickListAccess.invokeSaveTickList(this.toId, list, this.level.getGameTime());
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T object) {
        TickListSet<T> ticks = tickNextTickCubeMap.get(CubePos.asLong(pos));
        if (ticks == null) {
            return false;
        }
        return ticks.set.contains(new TickNextTickData<>(pos, object));
    }

    @Override
    public void scheduleTick(BlockPos pos, T object, int delay, TickPriority priority) {
        if (this.ignore.test(object)) {
            return;
        }
        TickListSet<T> ticks = cubeTickListFor(CubePos.asLong(pos));
        TickNextTickData<T> tick = new TickNextTickData<>(pos, object, delay + this.level.getGameTime(), priority);
        if (ticks.set.add(tick) && ticks.isFullyTicking()) {
            addToSortedList(tick);
        }
    }

    @Override
    public int size() {
        return tickNextTickSorted.size();
    }

    private static class TickListSet<T> {
        boolean entitiesLoaded, tickable;
        final Set<TickNextTickData<T>> set = new HashSet<>();

        boolean isFullyTicking() {
            return entitiesLoaded && tickable;
        }

        public boolean isUnloadable() {
            return !entitiesLoaded && !tickable && set.isEmpty();
        }
    }

    private static class TickListBucket<T> implements Comparable<TickListBucket<T>> {

        final long time;
        @SuppressWarnings("unchecked") final ArrayDeque<TickNextTickData<T>>[] byPriority = new ArrayDeque[TickPriority.values().length];

        TickListBucket(long time) {
            this.time = time;
            for (int i = 0; i < this.byPriority.length; i++) {
                this.byPriority[i] = new ArrayDeque<>();
            }
        }

        void add(TickNextTickData<T> data) {
            byPriority[data.priority.ordinal()].add(data);
        }

        public boolean remove(TickNextTickData<T> t) {
            return byPriority[t.priority.ordinal()].remove(t);
        }

        @Override public int compareTo(@NotNull CubicFastServerTickList.TickListBucket<T> o) {
            return Long.compare(time, o.time);
        }
    }
}