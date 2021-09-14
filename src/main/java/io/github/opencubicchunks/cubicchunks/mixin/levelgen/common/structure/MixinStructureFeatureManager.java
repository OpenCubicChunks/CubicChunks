package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.structure;

import static io.github.opencubicchunks.cubicchunks.utils.Coords.*;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelAccessor;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureFeatureManager.class)
public abstract class MixinStructureFeatureManager {

    @Shadow @Final private LevelAccessor level;

    @Shadow @Nullable public abstract StructureStart<?> getStartForFeature(SectionPos sectionPos, StructureFeature<?> structureFeature, FeatureAccess featureAccess);

    @Inject(at = @At("HEAD"), method = "startsForFeature(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/feature/StructureFeature;)Ljava/util/stream/Stream;",
        cancellable = true)
    private void useCubePos(SectionPos sectionPos, StructureFeature<?> structureFeature, CallbackInfoReturnable<Stream<? extends StructureStart<?>>> cir) {
        if (((CubicLevelHeightAccessor) level).generates2DChunks()) {
            return;
        }

        cir.setReturnValue(((CubicLevelAccessor) this.level).getCube(sectionToCube(sectionPos.x()), sectionToCube(sectionPos.y()), sectionToCube(sectionPos.z()), ChunkStatus.STRUCTURE_REFERENCES)
            .getReferencesForFeature(structureFeature).stream().map((seed) -> {
                return CubePos.from(seed);
            }).map((cubePos) -> {
                return this.getStartForFeature(null, structureFeature, ((CubicLevelAccessor) this.level).getCube(cubePos, ChunkStatus.STRUCTURE_STARTS));
            }).filter((structureStart) -> {
                return structureStart != null && structureStart.isValid();
            }));
    }
}
