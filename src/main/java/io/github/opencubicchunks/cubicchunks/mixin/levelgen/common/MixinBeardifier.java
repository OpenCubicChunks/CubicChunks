package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common;

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Beardifier;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Beardifier.class)
public class MixinBeardifier {

    @Nullable
    private ChunkAccess chunkAccess;

    @Redirect(method = "<init>(Lnet/minecraft/world/level/StructureFeatureManager;Lnet/minecraft/world/level/chunk/ChunkAccess;)V",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    private void setupChunkAccess(Stream stream, Consumer action, StructureFeatureManager structureFeatureManager, ChunkAccess chunk) {
        if (((CubicLevelHeightAccessor) chunk).generates2DChunks()) {
            stream.forEach(action);
            return;
        }

        this.chunkAccess = chunk;
        stream.forEach(action);
        this.chunkAccess = null;
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "lambda$new$1(Lnet/minecraft/world/level/ChunkPos;IILnet/minecraft/world/level/levelgen/structure/StructureStart;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/structures/JigsawJunction;getSourceX()I"))
    private int checkYBounds(JigsawJunction junction, ChunkPos pos, int number, int number2, StructureStart structureStart) {
        if (chunkAccess == null) {
            return junction.getSourceX();
        }

        int jigsawJunctionSourceY = junction.getSourceGroundY();
        int minY = chunkAccess.getMinBuildHeight();
        int maxY = chunkAccess.getMaxBuildHeight() - 1;
        boolean isInYBounds = jigsawJunctionSourceY > minY - 12 && jigsawJunctionSourceY < maxY + 12;

        if (isInYBounds) {
            return junction.getSourceX();
        } else {
            return Integer.MIN_VALUE;
        }
    }
}
