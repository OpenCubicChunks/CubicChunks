package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

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

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements SetupCubeStructureStart {


    @Shadow @Final protected List<StructurePiece> pieces;

    @Shadow protected abstract void calculateBoundingBox();

    @Override
    public void placeInCube(WorldGenLevel worldGenLevel, StructureFeatureManager structureFeatureManager, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox,
                            BlockPos cubePos) {
        synchronized(this.pieces) {
            if (!this.pieces.isEmpty()) {
                BoundingBox firstPieceBoundingBox = this.pieces.get(0).getBoundingBox();
                Vec3i centerPos = firstPieceBoundingBox.getCenter();
                BlockPos blockPos = new BlockPos(centerPos.getX(), firstPieceBoundingBox.y0, centerPos.getZ());
                Iterator<StructurePiece> iterator = this.pieces.iterator();


                while (iterator.hasNext()) {
                    StructurePiece structurePiece = iterator.next();
                    if (structurePiece.getBoundingBox().intersects(boundingBox) && !structurePiece
                        .postProcess(worldGenLevel, structureFeatureManager, chunkGenerator, random, boundingBox, null, blockPos)) {
                        iterator.remove();
                    }
                }
                this.calculateBoundingBox();
            }
        }
    }
}
