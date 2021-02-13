package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
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
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(ofLong, 1301), false);
        }
    }

    //TODO: Figure out what this is for.
    private LongSortedSet getCubeSections(int x, int y, int z) {
        long l = SectionPos.asLong(x, 0, z);
        long m = SectionPos.asLong(x, -1, z);
        return this.sectionIds.subSet(l, m + 1L);
    }
}
