package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.structure;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.world.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.world.SetupCubeStructureStart;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements SetupCubeStructureStart {


    @Shadow @Final protected List<StructurePiece> pieces;


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

    @Inject(method = "createTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;putInt(Ljava/lang/String;I)V", ordinal = 0), cancellable = true, locals =
        LocalCapture.CAPTURE_FAILHARD)
    private void packCubeStructureData(ServerLevel world, ChunkPos chunkPos, CallbackInfoReturnable<CompoundTag> cir, CompoundTag compoundTag) {

        if (!((CubicLevelHeightAccessor) world).isCubic()) {
            return;
        }

        if (chunkPos instanceof ImposterChunkPos) {
            compoundTag.putInt("ChunkY", ((ImposterChunkPos) chunkPos).y);
        }
    }
}
