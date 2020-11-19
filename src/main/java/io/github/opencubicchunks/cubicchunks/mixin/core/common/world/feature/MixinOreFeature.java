package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

//We do this because ores take up over half of world gen load time.
@Mixin(OreFeature.class)
public class MixinOreFeature {


    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void skipEmptyChunks(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, Random random, BlockPos blockPos, OreConfiguration oreConfiguration, CallbackInfoReturnable<Boolean> cir) {
        IBigCube cube = ((ICubicWorld) worldGenLevel).getCube(blockPos);
        LevelChunkSection section = cube.getCubeSections()[Coords.blockToIndex(blockPos.getX(), blockPos.getY(), blockPos.getZ())];
        if (section == null || section.isEmpty())
            cir.setReturnValue(true);
    }
}
