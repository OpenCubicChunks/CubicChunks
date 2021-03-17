package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure.pieces;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.ShipwreckPieces;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ShipwreckPieces.ShipwreckPiece.class)
public class MixinShipWreckPieces {

    @Shadow @Final private boolean isBeached;

    @Inject(method = "postProcess", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/levelgen/structure/ShipwreckPieces$ShipwreckPiece;isBeached:Z", ordinal = 1),
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private void isFloating(WorldGenLevel world, StructureFeatureManager structureAccessor, ChunkGenerator chunkGenerator, Random random, BoundingBox boundingBox, ChunkPos chunkPos,
                            BlockPos pos, CallbackInfoReturnable<Boolean> cir, int beachedHeight, int nonBeachedHeight, Vec3i vec3i) {
        if (((CubicLevelHeightAccessor) world).generates2DChunks()) {
            return;
        }
        if (isBeached) {
            if (beachedHeight < Coords.cubeToMinBlock(((CubeWorldGenRegion) world).getMainCubeY())) {
                cir.setReturnValue(false);
            }
        } else {
            if (nonBeachedHeight < Coords.cubeToMinBlock(((CubeWorldGenRegion) world).getMainCubeY())) {
                cir.setReturnValue(false);
            }
        }
    }
}
