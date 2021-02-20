package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.MineshaftFeature;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MineshaftFeature.MineShaftStart.class)
public class MixinMineshaftFeature {


    @Inject(method = "generatePieces",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/MineshaftFeature$MineShaftStart;moveBelowSeaLevel(IILjava/util/Random;I)V"))
    private void infiniteMineshafts(RegistryAccess registryAccess, ChunkGenerator chunkGenerator, StructureManager structureManager, int i, int j, Biome biome,
                                    MineshaftConfiguration mineshaftConfiguration, LevelHeightAccessor levelHeightAccessor, CallbackInfo ci) {

        BoundingBox boundingBox = ((MineshaftFeature.MineShaftStart) (Object) this).getBoundingBox();
        int cubeMaxY = levelHeightAccessor.getMinBuildHeight() + levelHeightAccessor.getHeight();

        if (boundingBox.y1 > cubeMaxY) {
            boundingBox.move(0, levelHeightAccessor.getMinBuildHeight(), 0);
        }

        for (StructurePiece piece : ((MineshaftFeature.MineShaftStart) (Object) this).getPieces()) {
            BoundingBox pieceBoundingBox = piece.getBoundingBox();
            if (pieceBoundingBox.y1 > cubeMaxY) {
                pieceBoundingBox.move(0, levelHeightAccessor.getMinBuildHeight(), 0);
            }
        }
    }
}
