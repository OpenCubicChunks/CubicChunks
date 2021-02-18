package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.surfacebuilder;

import java.util.Random;

import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.DefaultSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DefaultSurfaceBuilder.class)
public class MixinDefaultSurfaceBuilder {

    @ModifyConstant(
        method = "apply(Ljava/util/Random;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/biome/Biome;IIIDLnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;"
            + "Lnet/minecraft/world/level/block/state/BlockState;I)V", constant = @Constant(intValue = 50))
    private int doNotLoopOutsideCube(int arg0, Random random, ChunkAccess chunk, Biome biome, int x, int z, int height, double noise, BlockState defaultBlock, BlockState fluidBlock,
                                    BlockState topBlock, BlockState underBlock, BlockState underwaterBlock, int seaLevel) {

        if (chunk.getMinBuildHeight() > arg0) {
            return chunk.getMinBuildHeight();
        } else if (chunk.getMaxBuildHeight() < arg0) {
            return Integer.MAX_VALUE;
        } else {
            return arg0;
        }
    }
}