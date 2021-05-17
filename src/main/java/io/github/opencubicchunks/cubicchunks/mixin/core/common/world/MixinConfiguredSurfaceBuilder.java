package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.world.surfacebuilder.ChunkGeneratorFluidStateObtainer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.surfacebuilders.ConfiguredSurfaceBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConfiguredSurfaceBuilder.class)
public class MixinConfiguredSurfaceBuilder implements ChunkGeneratorFluidStateObtainer {

    private BlockState defaultFluidBlockState;

    @Inject(method = "apply", at = @At("HEAD"), cancellable = true)
    private void getDefaultFluidState(Random random, ChunkAccess chunk, Biome biome, int x, int z, int height, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel,
                                      int i, long l, CallbackInfo ci) {
        this.defaultFluidBlockState = defaultFluid;
    }

    @Override public BlockState getDefaultFluidBlockState() {
        return this.defaultFluidBlockState;
    }
}
