package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.utils.Utils;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.Util;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.ProcessorHandle;
import net.minecraft.util.thread.ProcessorMailbox;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.world.level.ChunkPos;

// TODO: use ChunkTaskPriorityQueueSorter internally with ImposterChunkPos
public class CubeTaskPriorityQueueSorter implements AutoCloseable, ChunkHolder.LevelChangeListener, CubeHolderLevelChangeListener {

    private final Map<ProcessorHandle<?>, CubeTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>>> queues;
    private final Set<ProcessorHandle<?>> actors;
    private final ProcessorMailbox<StrictQueue.IntRunnable> sorter;

    public CubeTaskPriorityQueueSorter(List<ProcessorHandle<?>> taskExecutors, Executor executor, int p_i50713_3_) {
        this.queues = taskExecutors.stream().collect(Collectors.toMap(Function.identity(), (p_219084_1_) ->
            new CubeTaskPriorityQueue<>(p_219084_1_.name() + "_queue", p_i50713_3_)));
        this.actors = Sets.newHashSet(taskExecutors);
        this.sorter = new ProcessorMailbox<>(new StrictQueue.FixedPriorityQueue(4), executor, "sorter");
    }

    // func_219069_a, message
    public static CubeTaskPriorityQueueSorter.FunctionEntry<Runnable> createMsg(Runnable runnable, long pos, IntSupplier p_219069_3_) {
        return new CubeTaskPriorityQueueSorter.FunctionEntry<>((p_219072_1_) -> () -> {
            runnable.run();
            p_219072_1_.tell(Unit.INSTANCE);
        }, pos, p_219069_3_);
    }

    // func_219081_a, message
    public static CubeTaskPriorityQueueSorter.FunctionEntry<Runnable> createMsg(ChunkHolder holder, Runnable p_219081_1_) {
        return createMsg(p_219081_1_, ((CubeHolder) holder).getCubePos().asLong(), holder::getQueueLevel);
    }

    // func_219073_a, release
    public static CubeTaskPriorityQueueSorter.RunnableEntry createSorterMsg(Runnable p_219073_0_, long p_219073_1_, boolean p_219073_3_) {
        return new CubeTaskPriorityQueueSorter.RunnableEntry(p_219073_0_, p_219073_1_, p_219073_3_);
    }

    // func_219087_a, getProcessor
    public <T> ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<T>> createExecutor(ProcessorHandle<T> iTaskExecutor, boolean p_219087_2_) {
        return this.sorter.<ProcessorHandle<CubeTaskPriorityQueueSorter.FunctionEntry<T>>>ask((p_219086_3_) -> new StrictQueue.IntRunnable(0, () -> {
            this.getQueue(iTaskExecutor);
            p_219086_3_.tell(ProcessorHandle.of("chunk priority sorter around " + iTaskExecutor.name(),
                (p_219071_3_) -> this.execute(iTaskExecutor, p_219071_3_.task, p_219071_3_.cubePos, p_219071_3_.level, p_219087_2_)));
        })).join();
    }

    // func_219091_a, getReleaseProcessor
    public ProcessorHandle<CubeTaskPriorityQueueSorter.RunnableEntry> createSorterExecutor(ProcessorHandle<Runnable> p_219091_1_) {
        return this.sorter.<ProcessorHandle<CubeTaskPriorityQueueSorter.RunnableEntry>>ask((p_219080_2_) -> new StrictQueue.IntRunnable(0, () -> p_219080_2_.tell(ProcessorHandle
            .of("chunk priority sorter around " + p_219091_1_.name(), (p_219075_2_) -> this.sort(p_219091_1_, p_219075_2_.pos, p_219075_2_.runnable, p_219075_2_.clearQueue))))).join();
    }

    // func_219066_a, onLevelChange
    @Override
    public void onCubeLevelChange(CubePos pos, IntSupplier getLevel, int level, IntConsumer setLevel) {
        this.sorter.tell(new StrictQueue.IntRunnable(0, () -> {
            int i = getLevel.getAsInt();
            this.queues.values().forEach((cubeTaskPriorityQueue) ->
                cubeTaskPriorityQueue.updateCubeLevel(i, pos, level));
            setLevel.accept(level);
        }));
    }

    // func_219074_a, release
    private <T> void sort(ProcessorHandle<T> p_219074_1_, long p_219074_2_, Runnable p_219074_4_, boolean p_219074_5_) {
        this.sorter.tell(new StrictQueue.IntRunnable(1, () -> {
            CubeTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> cubeTaskPriorityQueue = this.getQueue(p_219074_1_);
            cubeTaskPriorityQueue.clearPostion(p_219074_2_, p_219074_5_);
            if (this.actors.remove(p_219074_1_)) {
                this.pollTask(cubeTaskPriorityQueue, p_219074_1_);
            }

            p_219074_4_.run();
        }));
    }

    // func_219067_a, submit
    private <T> void execute(ProcessorHandle<T> p_219067_1_, Function<ProcessorHandle<Unit>, T> p_219067_2_, long p_219067_3_, IntSupplier p_219067_5_, boolean p_219067_6_) {
        this.sorter.tell(new StrictQueue.IntRunnable(2, () -> {
            CubeTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> cubeTaskPriorityQueue = this.getQueue(p_219067_1_);
            int i = p_219067_5_.getAsInt();
            cubeTaskPriorityQueue.add(Optional.of(p_219067_2_), p_219067_3_, i);
            if (p_219067_6_) {
                cubeTaskPriorityQueue.add(Optional.empty(), p_219067_3_, i);
            }

            if (this.actors.remove(p_219067_1_)) {
                this.pollTask(cubeTaskPriorityQueue, p_219067_1_);
            }

        }));
    }

    // func_219078_a, pollTask
    private <T> void pollTask(CubeTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> p_219078_1_, ProcessorHandle<T> p_219078_2_) {
        this.sorter.tell(new StrictQueue.IntRunnable(3, () -> {
            Stream<Either<Function<ProcessorHandle<Unit>, T>, Runnable>> stream = p_219078_1_.poll();
            if (stream == null) {
                this.actors.add(p_219078_2_);
            } else {
                Util.sequence(stream.map((p_219092_1_) -> p_219092_1_.map(p_219078_2_::ask, (p_219077_0_) -> {
                    p_219077_0_.run();
                    return CompletableFuture.completedFuture(Unit.INSTANCE);
                })).collect(Collectors.toList())).thenAccept((p_219088_3_) ->
                    this.pollTask(p_219078_1_, p_219078_2_));
            }

        }));
    }

    // getQueue
    private <T> CubeTaskPriorityQueue<Function<ProcessorHandle<Unit>, T>> getQueue(ProcessorHandle<T> p_219068_1_) {
        CubeTaskPriorityQueue<? extends Function<ProcessorHandle<Unit>, ?>> queue = this.queues.get(p_219068_1_);
        if (queue == null) {
            throw Util.pauseInIde((new IllegalArgumentException("No queue for: " + p_219068_1_)));
        } else {
            return Utils.unsafeCast(queue);
        }
    }

    // close
    public void close() {
        this.queues.keySet().forEach(ProcessorHandle::close);
    }

    @Override
    public void onLevelChange(ChunkPos pos, IntSupplier p_219066_2_, int p_219066_3_, IntConsumer p_219066_4_) {
        throw new AbstractMethodError("This function should never be called! EVER");
    }

    public static final class FunctionEntry<T> {
        private final Function<ProcessorHandle<Unit>, T> task;
        private final long cubePos;
        private final IntSupplier level;

        private FunctionEntry(Function<ProcessorHandle<Unit>, T> p_i50028_1_, long p_i50028_2_, IntSupplier p_i50028_4_) {
            this.task = p_i50028_1_;
            this.cubePos = p_i50028_2_;
            this.level = p_i50028_4_;
        }
    }

    public static final class RunnableEntry {
        private final Runnable runnable;
        private final long pos;
        private final boolean clearQueue;

        private RunnableEntry(Runnable runnable, long pos, boolean p_i50026_4_) {
            this.runnable = runnable;
            this.pos = pos;
            this.clearQueue = p_i50026_4_;
        }
    }
}