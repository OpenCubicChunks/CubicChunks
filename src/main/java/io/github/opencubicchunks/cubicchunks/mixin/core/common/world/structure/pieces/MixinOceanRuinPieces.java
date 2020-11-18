package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure.pieces;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.OceanRuinPieces;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(OceanRuinPieces.OceanRuinPiece.class)
public abstract class MixinOceanRuinPieces extends TemplateStructurePiece {

    @Shadow @Final private float integrity;

    public MixinOceanRuinPieces(StructurePieceType structurePieceType, int i) {
        super(structurePieceType, i);
    }

    @Inject(at = @At("HEAD"), method = "postProcess", cancellable = true)
    private void patchYPosition(WorldGenLevel worldGenLevel, StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox, ChunkPos chunkPos, BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        if (!(worldGenLevel instanceof CubeWorldGenRegion))
            return;
            cir.cancel();
            this.placeSettings.clearProcessors().addProcessor(new BlockRotProcessor(this.integrity)).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
            int i = worldGenLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
            this.templatePosition = new BlockPos(this.templatePosition.getX(), i, this.templatePosition.getZ());
            cir.setReturnValue(super.postProcess(worldGenLevel, structureFeatureManager, chunkGenerator, random, boundingBox, chunkPos, blockPos));
    }
}
