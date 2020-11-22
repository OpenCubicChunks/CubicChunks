package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructurePiece.class)
public class MixinStructurePiece {

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;getChunk(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/chunk/ChunkAccess;"),
        method = "placeBlock")
    private ChunkAccess getCube(WorldGenLevel worldGenLevel, BlockPos blockPos) {
        return (ChunkAccess) ((ICubicWorld) worldGenLevel).getCube(blockPos);
    }
}
