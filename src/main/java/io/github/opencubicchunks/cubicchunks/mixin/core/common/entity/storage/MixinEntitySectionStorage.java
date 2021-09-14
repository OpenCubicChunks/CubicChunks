package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.LevelCube;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.chunk.entity.IsCubicEntityContext;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntitySectionStorage.class)
public abstract class MixinEntitySectionStorage<T extends EntityAccess> implements IsCubicEntityContext {

    @Shadow @Final private LongSortedSet sectionIds;

    private boolean isCubic;

    @Shadow private static long getChunkKeyFromSectionKey(long sectionPos) {
        throw new Error("Mixin did not apply");
    }


    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntitySectionStorage;getChunkKeyFromSectionKey(J)J"))
    private long getCubeKeyFromSectionKey(long sectionKey) {
        if (isCubic) {
            return CubePos.asLong(Coords.sectionToCube(SectionPos.x(sectionKey)), Coords.sectionToCube(SectionPos.y(sectionKey)), Coords.sectionToCube(SectionPos.z(sectionKey)));
        } else {
            return getChunkKeyFromSectionKey(sectionKey);
        }
    }

    @Inject(method = "getExistingSectionPositionsInChunk", at = @At("HEAD"), cancellable = true)
    public void getExistingSectionPositionsInChunk(long cubePos, CallbackInfoReturnable<LongStream> cir) {
        if (!isCubic) {
            return;
        }

        int x = CubePos.extractX(cubePos);
        int y = CubePos.extractY(cubePos);
        int z = CubePos.extractZ(cubePos);
        LongSortedSet longSortedSet = this.getCubeSections(x, y, z);
        if (longSortedSet.isEmpty()) {
            cir.setReturnValue(LongStream.empty());
        } else {
            PrimitiveIterator.OfLong ofLong = longSortedSet.iterator();
            cir.setReturnValue(StreamSupport.longStream(Spliterators.spliteratorUnknownSize(ofLong,
                Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false));
        }
    }

    // Important: these coordinates are cube coordinates, not section coordinates
    private LongSortedSet getCubeSections(int cubeX, int cubeY, int cubeZ) {
        int sectionX = Coords.cubeToSection(cubeX, 0);
        int sectionY = Coords.cubeToSection(cubeY, 0);
        int sectionZ = Coords.cubeToSection(cubeZ, 0);

        LongSortedSet set = new LongAVLTreeSet();

        for (int relX = 0; relX < LevelCube.DIAMETER_IN_SECTIONS; relX++) {
            for (int relZ = 0; relZ < LevelCube.DIAMETER_IN_SECTIONS; relZ++) {
                for (int relY = 0; relY < LevelCube.DIAMETER_IN_SECTIONS; relY++) {

                    long sectionPos = SectionPos.asLong(sectionX + relX, sectionY + relY, sectionZ + relZ);
                    if (this.sectionIds.contains(sectionPos)) {
                        set.add(sectionPos);
                    }
                }
            }
        }

        return set;
    }

    @Override public boolean isCubic() {
        return this.isCubic;
    }

    @Override public void setIsCubic(boolean isCubic) {
        this.isCubic = isCubic;
    }
}
