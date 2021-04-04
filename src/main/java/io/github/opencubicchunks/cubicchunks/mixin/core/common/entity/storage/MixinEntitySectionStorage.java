package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage {


    @Shadow @Final private LongSortedSet sectionIds;

    /**
     * @author CorgiTaco
     * @reason UseCubePos
     */
    @Overwrite
    private static long getChunkKeyFromSectionKey(long l) {
        return CubePos.asLong(SectionPos.x(l), SectionPos.y(l), SectionPos.z(l));
    }

    /**
     * @author CorgiTaco
     * @reason UseCubePos
     */
    @Overwrite
    public LongStream getExistingSectionPositionsInChunk(long l) {
        int x = CubePos.extractX(l);
        int y = CubePos.extractX(l);
        int z = CubePos.extractZ(l);
        LongSortedSet longSortedSet = this.getCubeSections(x, y, z);
        if (longSortedSet.isEmpty()) {
            return LongStream.empty();
        } else {
            PrimitiveIterator.OfLong ofLong = longSortedSet.iterator();
            // 1301 == Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.ORDERED | Spliterator.SORTED | Spliterator.DISTINCT
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(ofLong, 1301), false);
        }
    }

    // Important: these coordinates are cube coordinates, not section coordinates
    private LongSortedSet getCubeSections(int x, int y, int z) {
        long l = SectionPos.asLong(x, 0, z);
        long m = SectionPos.asLong(x, -1, z);
        return this.sectionIds.subSet(l, m + 1L);
//        SectionPos pos = CubePos.of(x, y, z).asSectionPos();
//
//        LongSortedSet set = new LongAVLTreeSet();
//
//        for (int relX = 0; relX < BigCube.DIAMETER_IN_SECTIONS; relX++) {
//            for (int relZ = 0; relZ < BigCube.DIAMETER_IN_SECTIONS; relZ++) {
//                for (int relY = 0; relY < BigCube.DIAMETER_IN_SECTIONS; relY++) {
//                    long sectionPos = pos.offset(relX, relY, relZ).asLong();
//                    if (this.sectionIds.contains(sectionPos)) {
//                        set.add(sectionPos);
//                    }
//                }
//            }
//        }
//
//        return set;
    }
}
