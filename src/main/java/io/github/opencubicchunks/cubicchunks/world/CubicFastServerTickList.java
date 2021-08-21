package io.github.opencubicchunks.cubicchunks.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import io.github.opencubicchunks.cubicchunks.chunk.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.ServerTickListAccess;
import io.github.opencubicchunks.cubicchunks.world.entity.ChunkEntityStateEventHandler;
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

public class CubicFastServerTickList<T> extends ServerTickList<T> implements ChunkEntityStateEventHandler {
    private final Function<T, ResourceLocation> toId;

    private final ServerLevel level;
    private final Consumer<TickNextTickData<T>> ticker;

    private final Map<CubePos, TickListSet<T>> tickNextTickSet = new HashMap<>();
    private final Set<TickNextTickData<T>> tickNextTickList = Sets.newTreeSet(TickNextTickData.createTimeComparator());

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
        TickListSet<T> ticks = tickNextTickSet.computeIfAbsent(pos, x -> new TickListSet<>());
        assert !ticks.isFullyTicking();
        ticks.entitiesLoaded = true;
        if (ticks.isFullyTicking()) {
            tickNextTickList.addAll(ticks.set);
        }
    }

    @Override public void onCubeEntitiesUnload(CubePos pos) {
        TickListSet<T> ticks = tickNextTickSet.computeIfAbsent(pos, x -> new TickListSet<>());
        boolean prevTicking = ticks.isFullyTicking();
        ticks.entitiesLoaded = false;
        if (prevTicking) {
            tickNextTickList.removeAll(ticks.set);
        }
    }

    public void onCubeStartTicking(CubePos pos) {
        TickListSet<T> ticks = tickNextTickSet.computeIfAbsent(pos, x -> new TickListSet<>());
        assert !ticks.isFullyTicking();
        ticks.tickable = true;
        if (ticks.isFullyTicking()) {
            tickNextTickList.addAll(ticks.set);
        }
    }

    public void onCubeStopTicking(CubePos pos) {
        TickListSet<T> ticks = tickNextTickSet.computeIfAbsent(pos, x -> new TickListSet<>());
        boolean prevTicking = ticks.isFullyTicking();
        ticks.tickable = false;
        if (prevTicking) {
            tickNextTickList.removeAll(ticks.set);
        }
    }

    @Override
    public void tick() {
        int count = this.tickNextTickList.size();

        if (count > 65536) {
            count = 65536;
        }

        this.level.getProfiler().push("cleaning");
        Iterator<TickNextTickData<T>> it = this.tickNextTickList.iterator();
        while (count > 0 && it.hasNext()) {
            TickNextTickData<T> tickData = it.next();
            if (tickData.triggerTick > this.level.getGameTime()) {
                break;
            }
            it.remove();
            this.tickNextTickSet.get(CubePos.from(tickData.pos)).set.remove(tickData);
            this.currentlyTicking.add(tickData);
            --count;
        }

        this.level.getProfiler().popPush("ticking");

        TickNextTickData<T> tickData;
        while ((tickData = this.currentlyTicking.poll()) != null) {
            try {
                //this.alreadyTicked.add(tickData);
                this.ticker.accept(tickData);
            } catch (Throwable ex) {
                CrashReport report = CrashReport.forThrowable(ex, "Exception while ticking");
                CrashReportCategory crashReportCategory = report.addCategory("Block being ticked");
                CrashReportCategory.populateBlockDetails(crashReportCategory, this.level, tickData.pos, null);
                throw new ReportedException(report);
            }
        }

        this.level.getProfiler().pop();
        // this.alreadyTicked.clear();
        this.currentlyTicking.clear();
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

    public List<TickNextTickData<T>> fetchTicksInCube(CubePos pos, boolean updateState, boolean getStaleTicks) {
        // TODO: do we really need currentlyTicking and alreadyTicked here?
        TickListSet<T> ticks = tickNextTickSet.get(pos);
        if (ticks == null) {
            return new ArrayList<>();
        }
        ArrayList<TickNextTickData<T>> output = new ArrayList<>(ticks.set);
        if (updateState) {
            tickNextTickSet.remove(pos);
            if (ticks.isFullyTicking()) {
                output.forEach(tickNextTickList::remove);
            }
        }
        return output;
    }

    @Override
    public List<TickNextTickData<T>> fetchTicksInArea(BoundingBox bounds, boolean updateState, boolean getStaleTicks) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void copy(BoundingBox box, BlockPos offset) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // vanilla save() is fine

    public ListTag save(CubePos chunkPos) {
        List<TickNextTickData<T>> list = this.fetchTicksInCube(chunkPos, false, true);
        return ServerTickListAccess.saveTickList(this.toId, list, this.level.getGameTime());
    }

    @Override
    public boolean hasScheduledTick(BlockPos pos, T object) {
        CubePos cubePos = CubePos.from(pos);
        TickListSet<T> ticks = tickNextTickSet.get(cubePos);
        if (ticks == null) {
            return false;
        }
        return ticks.set.contains(new TickNextTickData<>(pos, object));
    }

    @Override
    public void scheduleTick(BlockPos pos, T object, int delay, TickPriority priority) {
        CubePos cubePos = CubePos.from(pos);
        TickListSet<T> ticks = tickNextTickSet.computeIfAbsent(cubePos, x -> new TickListSet<>());
        TickNextTickData<T> tick = new TickNextTickData<>(pos, object, delay + this.level.getGameTime(), priority);
        ticks.set.add(tick);
        if (ticks.isFullyTicking()) {
            tickNextTickList.add(tick);
        }
    }

    @Override
    public int size() {
        // TODO: should we include untickable?
        return tickNextTickList.size();
    }

    private static class TickListSet<T> {
        boolean entitiesLoaded, tickable;
        final Set<TickNextTickData<T>> set = new HashSet<>();

        boolean isFullyTicking() {
            return entitiesLoaded && tickable;
        }
    }
}