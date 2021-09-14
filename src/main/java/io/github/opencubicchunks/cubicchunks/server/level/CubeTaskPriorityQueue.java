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

    protected void updateCubeLevel(int p_219407_1_, CubePos pos, int p_219407_3_) {
        if (p_219407_1_ < LEVEL_COUNT) {
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap = this.levelToPosToElements.get(p_219407_1_);
            List<Optional<T>> list = long2objectlinkedopenhashmap.remove(pos.asLong());
            if (p_219407_1_ == this.firstNonEmptyLvl) {
                while (this.firstNonEmptyLvl < LEVEL_COUNT && this.levelToPosToElements.get(this.firstNonEmptyLvl).isEmpty()) {
                    ++this.firstNonEmptyLvl;
                }
            }

            if (list != null && !list.isEmpty()) {
                this.levelToPosToElements.get(p_219407_3_).computeIfAbsent(pos.asLong(), (p_219411_0_) -> Lists.newArrayList()).addAll(list);
                this.firstNonEmptyLvl = Math.min(this.firstNonEmptyLvl, p_219407_3_);
            }

        }
    }

    protected void add(Optional<T> p_219412_1_, long p_219412_2_, int p_219412_4_) {
        this.levelToPosToElements.get(p_219412_4_).computeIfAbsent(p_219412_2_, (p_219410_0_) -> Lists.newArrayList()).add(p_219412_1_);
        this.firstNonEmptyLvl = Math.min(this.firstNonEmptyLvl, p_219412_4_);
    }

    protected void clearPostion(long p_219416_1_, boolean p_219416_3_) {
        for (Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap : this.levelToPosToElements) {
            List<Optional<T>> list = long2objectlinkedopenhashmap.get(p_219416_1_);
            if (list != null) {
                if (p_219416_3_) {
                    list.clear();
                } else {
                    list.removeIf((p_219413_0_) -> !p_219413_0_.isPresent());
                }

                if (list.isEmpty()) {
                    long2objectlinkedopenhashmap.remove(p_219416_1_);
                }
            }
        }

        while (this.firstNonEmptyLvl < LEVEL_COUNT && this.levelToPosToElements.get(this.firstNonEmptyLvl).isEmpty()) {
            ++this.firstNonEmptyLvl;
        }

        this.cubePostions.remove(p_219416_1_);
    }

    private Runnable createCubePositionAdder(long p_219418_1_) {
        return () -> {
            this.cubePostions.add(p_219418_1_);
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
            Long2ObjectLinkedOpenHashMap<List<Optional<T>>> long2objectlinkedopenhashmap = this.levelToPosToElements.get(i);
            long j = long2objectlinkedopenhashmap.firstLongKey();

            List<Optional<T>> list;
            list = long2objectlinkedopenhashmap.removeFirst();
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