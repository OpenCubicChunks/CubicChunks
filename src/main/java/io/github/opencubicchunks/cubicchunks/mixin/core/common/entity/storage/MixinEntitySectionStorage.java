package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import java.util.PrimitiveIterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntitySectionStorage.class)
public class MixinEntitySectionStorage<T extends EntityAccess> {


    @Shadow @Final private LongSortedSet sectionIds;

    @Shadow @Final private Long2ObjectMap<EntitySection<T>> sections;

    /**
     * @author CorgiTaco
     * @reason UseCubePos
     */
    @Overwrite
    private static long getChunkKeyFromSectionKey(long l) {
        return CubePos.asLong(SectionPos.x(l), SectionPos.y(l), SectionPos.z(l));
    }

    /**
     * @author Corgi Taco
     */
    @Overwrite
    public void forEachAccessibleSection(AABB box, Consumer<EntitySection<T>> action) {
        int bbMinX = SectionPos.posToSectionCoord(box.minX - 2.0D);
        int bbMinY = SectionPos.posToSectionCoord(box.minY - 2.0D);
        int bbMinZ = SectionPos.posToSectionCoord(box.minZ - 2.0D);
        int bbMaxX = SectionPos.posToSectionCoord(box.maxX + 2.0D);
        int bbMaxY = SectionPos.posToSectionCoord(box.maxY + 2.0D);
        int bbMaxZ = SectionPos.posToSectionCoord(box.maxZ + 2.0D);

        for (int currentX = bbMinX; currentX <= bbMaxX; ++currentX) {
            //Use the BB x as keys for accessing the to and from
            long fromSection = SectionPos.asLong(currentX, 0, 0);
            long toSection = SectionPos.asLong(currentX, -1, -1);

            for (long sectionID : this.sectionIds.subSet(fromSection, toSection + 1L)) {
                int currentY = SectionPos.y(sectionID);
                int currentZ = SectionPos.z(sectionID);

                boolean isInYBounds = currentY >= bbMinY && currentY <= bbMaxY;
                boolean isInZBounds = currentZ >= bbMinZ && currentZ <= bbMaxZ;
                SectionPos sectionPos = SectionPos.of(sectionID);
                if (isInYBounds && isInZBounds) {
                    EntitySection<T> entitySection = this.sections.get(sectionID);
                    if (entitySection != null && entitySection.getStatus().isAccessible()) {
                        action.accept(entitySection);
                    }
                }
            }
        }
    }

    /**
     * @author CorgiTaco
     * @reason UseCubePos
     */
    @Overwrite
    public LongStream getExistingSectionPositionsInChunk(long cubePos) {
        int x = CubePos.extractX(cubePos);
        int y = CubePos.extractX(cubePos);
        int z = CubePos.extractZ(cubePos);
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
//        long l = SectionPos.asLong(x, 0, z);
//        long m = SectionPos.asLong(x, -1, z);
//        return this.sectionIds.subSet(l, m + 1L);
        SectionPos pos = CubePos.of(x, y, z).asSectionPos();

        LongSortedSet set = new LongAVLTreeSet();

        for (int relX = 0; relX < BigCube.DIAMETER_IN_SECTIONS; relX++) {
            for (int relZ = 0; relZ < BigCube.DIAMETER_IN_SECTIONS; relZ++) {
                for (int relY = 0; relY < BigCube.DIAMETER_IN_SECTIONS; relY++) {
                    long sectionPos = pos.offset(relX, relY, relZ).asLong();
                    if (this.sectionIds.contains(sectionPos)) {
                        set.add(sectionPos);
                    }
                }
            }
        }

        return set;
    }
}
