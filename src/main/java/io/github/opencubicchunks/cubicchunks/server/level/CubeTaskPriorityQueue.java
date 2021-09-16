package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;

// TODO: use ChunkTaskPriorityQueue internally, with imposter chunk pos
public class CubeTaskPriorityQueue<T> {
    public static final int LEVEL_COUNT = CubeMap.MAX_CUBE_DISTANCE + 2;
    private final List<Long2ObjectLinkedOpenHashMap<List<Optional<T>>>> levelToPosToElements =
        IntStream.range(0, LEVEL_COUNT).mapToObj((p_219415_0_) -> new Long2ObjectLinkedOpenHashMap<List<Optional<T>>>()).collect(Collectors.toList());
    private volatile int firstNonEmptyLvl = LEVEL_COUNT;
    private final String name;
    private final LongSet cubePostions = new LongOpenHashSet();
    private final int sizeMax;

    public CubeTaskPriorityQueue(String name, int maxSize) {
        this.name = name;
        this.sizeMax = maxSize;
    }

    protected void updateCubeLevel(int oldLevel, CubePos pos, int newLevel) {
        if (oldLevel < LEVEL_COUNT) {
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> elements = this.levelToPosToElements.get(oldLevel);
            List<Optional<T>> list = elements.remove(pos.asLong());
            if (oldLevel == this.firstNonEmptyLvl) {
                while (this.firstNonEmptyLvl < LEVEL_COUNT && this.levelToPosToElements.get(this.firstNonEmptyLvl).isEmpty()) {
                    ++this.firstNonEmptyLvl;
                }
            }
            if (list != null && !list.isEmpty()) {
                this.levelToPosToElements.get(newLevel).computeIfAbsent(pos.asLong(), i -> Lists.newArrayList()).addAll(list);
                this.firstNonEmptyLvl = Math.min(this.firstNonEmptyLvl, newLevel);
            }
        }
    }

    protected void add(Optional<T> value, long pos, int level) {
        this.levelToPosToElements.get(level).computeIfAbsent(pos, (p_219410_0_) -> Lists.newArrayList()).add(value);
        this.firstNonEmptyLvl = Math.min(this.firstNonEmptyLvl, level);
    }

    protected void clearPostion(long pos, boolean bl) {
        for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap : this.levelToPosToElements) {
            List<Optional<T>> list = long2objectlinkedopenhashmap.get(pos);
            if (list != null) {
                if (bl) {
                    list.clear();
                } else {
                    list.removeIf((p_219413_0_) -> !p_219413_0_.isPresent());
                }

                if (list.isEmpty()) {
                    long2objectlinkedopenhashmap.remove(pos);
                }
            }
        }

        while (this.firstNonEmptyLvl < LEVEL_COUNT && this.levelToPosToElements.get(this.firstNonEmptyLvl).isEmpty()) {
            ++this.firstNonEmptyLvl;
        }

        this.cubePostions.remove(pos);
    }

    private Runnable createCubePositionAdder(long pos) {
        return () -> {
            this.cubePostions.add(pos);
        };
    }

    @Nullable
    public Stream<Either<T, Runnable>> poll() {
        if (this.cubePostions.size() >= this.sizeMax) {
            return null;
        } else if (this.firstNonEmptyLvl >= LEVEL_COUNT) {
            return null;
        } else {
            int i = this.firstNonEmptyLvl;
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> elements = this.levelToPosToElements.get(i);
            long j = elements.firstLongKey();

            List<Optional<T>> list;
            list = elements.removeFirst();
            while (this.firstNonEmptyLvl < LEVEL_COUNT && this.levelToPosToElements.get(this.firstNonEmptyLvl).isEmpty()) {
                ++this.firstNonEmptyLvl;
            }
            return list.stream().map((p_219408_3_) -> p_219408_3_.<Either<T, Runnable>>map(Either::left).orElseGet(() -> {
                return Either.right(this.createCubePositionAdder(j));
            }));
        }
    }

    public String toString() {
        return this.name + " " + this.firstNonEmptyLvl + "...";
    }
}