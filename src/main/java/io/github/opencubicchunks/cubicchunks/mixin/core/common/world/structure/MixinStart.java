package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import io.github.opencubicchunks.cubicchunks.world.SetupCubeStructureStart;
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

import java.util.Iterator;
import java.util.List;
import java.util.Random;

@Mixin(StructureStart.class)
public abstract class MixinStart implements SetupCubeStructureStart {


    @Shadow @Final protected List<StructurePiece> pieces;

    @Shadow protected abstract void calculateBoundingBox();

    @Override
    public void placeInCube(WorldGenLevel worldGenLevel, StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox, BlockPos cubePos) {
        synchronized(this.pieces) {
            if (!this.pieces.isEmpty()) {
                BoundingBox firstPieceBoundingBox = this.pieces.get(0).getBoundingBox();
                Vec3i centerPosFrom1stPieceBoundingBox = firstPieceBoundingBox.getCenter();                   //Divide by 8 here i.e a y of 64 is now 8.
                BlockPos blockPos = new BlockPos(centerPosFrom1stPieceBoundingBox.getX(), cubePos.getY() + (firstPieceBoundingBox.y0 / 8)/*Add to the original y value from the cube section/column corner pos*/, centerPosFrom1stPieceBoundingBox.getZ());
                Iterator<StructurePiece> iterator = this.pieces.iterator();

                while(iterator.hasNext()) {
                    StructurePiece structurePiece = iterator.next();
                    if (structurePiece.getBoundingBox().intersects(boundingBox) && !structurePiece.postProcess(worldGenLevel, structureFeatureManager, chunkGenerator, random, boundingBox, null, blockPos)) {
                        iterator.remove();
                    }
                }
                this.calculateBoundingBox();
            }
        }
    }
}
