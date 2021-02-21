package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.world.ICubicStructureStart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements ICubicStructureStart {

    private int sectionY;
    private boolean has3dPlacement;

    @Shadow @Final protected List<StructurePiece> pieces;


    @Shadow protected BoundingBox boundingBox;

    @Override public void init3dPlacement(int sectionY) {
        this.sectionY = sectionY;
        this.has3dPlacement = true;
    }

    @Override public boolean has3DPlacement() {
        return this.has3dPlacement;
    }

    @Override
    public void placeInCube(WorldGenLevel worldGenLevel, StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox,
                            BlockPos cubePos) {

        synchronized(this.pieces) {
            if (!this.pieces.isEmpty()) {
                BoundingBox firstPieceBoundingBox = this.pieces.get(0).getBoundingBox();
                Vec3i centerPos = firstPieceBoundingBox.getCenter();
                BlockPos blockPos = new BlockPos(centerPos.getX(), firstPieceBoundingBox.minY(), centerPos.getZ());
                Iterator<StructurePiece> iterator = this.pieces.iterator();


                while (iterator.hasNext()) {
                    StructurePiece structurePiece = iterator.next();
                    if (structurePiece.getBoundingBox().intersects(boundingBox) && !structurePiece
                        .postProcess(worldGenLevel, structureFeatureManager, chunkGenerator, random, boundingBox, null, blockPos)) {
                        iterator.remove();
                    }
                }
//                this.calculateBoundingBox();
            }
        }
    }

    @ModifyArg(method = "getLocatePos", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;<init>(III)V"), index = 1)
    private int getStructureY(int arg0) {
        return (this.boundingBox.y0 + this.boundingBox.y1) >> 1;
    }
}
