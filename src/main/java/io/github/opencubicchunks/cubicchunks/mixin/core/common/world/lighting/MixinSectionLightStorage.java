package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.LevelBasedGraphAccess;
import io.github.opencubicchunks.cubicchunks.world.lighting.ISectionLightStorage;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.Direction;
import net.minecraft.util.SectionDistanceGraph;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.LightDataMap;
import net.minecraft.world.lighting.LightEngine;
import net.minecraft.world.lighting.SectionLightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(SectionLightStorage.class)
public abstract class MixinSectionLightStorage <M extends LightDataMap<M>> extends SectionDistanceGraph implements ISectionLightStorage {

    @Shadow protected abstract boolean hasSectionsToUpdate();

    @Shadow @Final protected Long2ObjectMap<NibbleArray> newArrays;
    @Shadow @Final private LongSet noLightSections;

    @Shadow protected abstract void cancelSectionUpdates(LightEngine<?, ?> engine, long sectionPosIn);

    @Shadow @Final protected M cachedLightData;

    @Shadow(aliases = "onNodeRemoved") protected abstract void removeSection(long p_215523_1_);

    @Shadow protected volatile boolean hasSectionsToUpdate;

    @Shadow protected abstract boolean hasSection(long sectionPosIn);

    @Shadow @Final protected LongSet dirtyCachedSections;

    @Shadow @Final private static Direction[] DIRECTIONS;

    @Shadow protected abstract void processAllLevelUpdates();

    private final LongSet cubesToRetain = new LongOpenHashSet();

    protected MixinSectionLightStorage(int p_i50706_1_, int p_i50706_2_, int p_i50706_3_) {
        super(p_i50706_1_, p_i50706_2_, p_i50706_3_);
    }

    @Override
    public void retainCubeData(long cubeSectionPos, boolean retain) {
        if (retain) {
            this.cubesToRetain.add(cubeSectionPos);
        } else {
            this.cubesToRetain.remove(cubeSectionPos);
        }
    }

    /**
     * @author NotStirred
     * @reason entire method was chunk based
     */
    @Overwrite
    protected void updateSections(LightEngine<M, ?> engine, boolean updateSkyLight, boolean updateBlockLight) {
        if (this.hasSectionsToUpdate() || !this.newArrays.isEmpty()) {
            for(long noLightPos : this.noLightSections) {
                this.cancelSectionUpdates(engine, noLightPos);
                NibbleArray nibblearray = this.newArrays.remove(noLightPos);
                NibbleArray nibblearray1 = this.cachedLightData.removeLayer(noLightPos);
                if (this.cubesToRetain.contains(CubePos.sectionToCubeSectionLong(noLightPos))) {
                    if (nibblearray != null) {
                        this.newArrays.put(noLightPos, nibblearray);
                    } else if (nibblearray1 != null) {
                        this.newArrays.put(noLightPos, nibblearray1);
                    }
                }
            }

            this.cachedLightData.clearCache();

            for(long section : this.noLightSections) {
                //TODO: implement this for CC
                this.removeSection(section);
            }

            this.noLightSections.clear();
            this.hasSectionsToUpdate = false;

            for(Long2ObjectMap.Entry<NibbleArray> entry : this.newArrays.long2ObjectEntrySet()) {
                long entryPos = entry.getLongKey();
                if (this.hasSection(entryPos)) {
                    NibbleArray nibblearray2 = entry.getValue();
                    if (this.cachedLightData.getLayer(entryPos) != nibblearray2) {
                        this.cancelSectionUpdates(engine, entryPos);
                        this.cachedLightData.setLayer(entryPos, nibblearray2);
                        this.dirtyCachedSections.add(entryPos);
                    }
                }
            }

            LevelBasedGraphAccess engineAccess = ((LevelBasedGraphAccess)engine);

            this.cachedLightData.clearCache();
            if (!updateBlockLight) {
                for(long newArray : this.newArrays.keySet()) {
                    if (this.hasSection(newArray)) {
                        int newX = SectionPos.sectionToBlockCoord(SectionPos.x(newArray));
                        int newY = SectionPos.sectionToBlockCoord(SectionPos.y(newArray));
                        int newZ = SectionPos.sectionToBlockCoord(SectionPos.z(newArray));

                        for(Direction direction : DIRECTIONS) {
                            long posOffset = SectionPos.offset(newArray, direction);
                            if (!this.newArrays.containsKey(posOffset) && this.hasSection(posOffset)) {
                                for(int i1 = 0; i1 < 16; ++i1) {
                                    for(int j1 = 0; j1 < 16; ++j1) {
                                        long k1;
                                        long l1;
                                        switch(direction) {
                                            case DOWN:
                                                k1 = BlockPos.asLong(newX + j1, newY, newZ + i1);
                                                l1 = BlockPos.asLong(newX + j1, newY - 1, newZ + i1);
                                                break;
                                            case UP:
                                                k1 = BlockPos.asLong(newX + j1, newY + 16 - 1, newZ + i1);
                                                l1 = BlockPos.asLong(newX + j1, newY + 16, newZ + i1);
                                                break;
                                            case NORTH:
                                                k1 = BlockPos.asLong(newX + i1, newY + j1, newZ);
                                                l1 = BlockPos.asLong(newX + i1, newY + j1, newZ - 1);
                                                break;
                                            case SOUTH:
                                                k1 = BlockPos.asLong(newX + i1, newY + j1, newZ + 16 - 1);
                                                l1 = BlockPos.asLong(newX + i1, newY + j1, newZ + 16);
                                                break;
                                            case WEST:
                                                k1 = BlockPos.asLong(newX, newY + i1, newZ + j1);
                                                l1 = BlockPos.asLong(newX - 1, newY + i1, newZ + j1);
                                                break;
                                            default:
                                                k1 = BlockPos.asLong(newX + 16 - 1, newY + i1, newZ + j1);
                                                l1 = BlockPos.asLong(newX + 16, newY + i1, newZ + j1);
                                        }

                                        engineAccess.invokeScheduleUpdate(k1, l1, engineAccess.invokeGetEdgeLevel(k1, l1, engineAccess.invokeGetLevel(k1)),
                                                false);
                                        engineAccess.invokeScheduleUpdate(l1, k1, engineAccess.invokeGetEdgeLevel(l1, k1, engineAccess.invokeGetLevel(l1)),
                                                false);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ObjectIterator<Long2ObjectMap.Entry<NibbleArray>> objectiterator = this.newArrays.long2ObjectEntrySet().iterator();

            while(objectiterator.hasNext()) {
                Long2ObjectMap.Entry<NibbleArray> entry1 = objectiterator.next();
                long k2 = entry1.getLongKey();
                if (this.hasSection(k2)) {
                    objectiterator.remove();
                }
            }

        }
    }

}