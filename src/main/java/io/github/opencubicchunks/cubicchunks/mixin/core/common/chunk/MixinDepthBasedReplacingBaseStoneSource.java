package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DepthBasedReplacingBaseStoneSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DepthBasedReplacingBaseStoneSource.class)
public class MixinDepthBasedReplacingBaseStoneSource {

    @Shadow @Final private BlockState replacementBlock;

    @Shadow @Final private WorldgenRandom random;

    @Shadow @Final private BlockState normalBlock;

    @Shadow @Final private long seed;

    @Inject(method = "getBaseStone", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setBaseStoneSeed(JIII)J", shift = At.Shift.BEFORE),
        cancellable = true)
    private void dontCalculateBaseStone(int i, int j, int k, NoiseGeneratorSettings noiseGeneratorSettings, CallbackInfoReturnable<BlockState> cir) {
        double probability = Mth.clampedMap(j, -8.0D, 0.0D, 1.0D, 0.0D);
        if (probability <= 0) {
            cir.setReturnValue(this.normalBlock);
        } else if (probability >= 1) {
            cir.setReturnValue(this.replacementBlock);
        } else {
            this.random.setBaseStoneSeed(this.seed, i, j, k);
            cir.setReturnValue((double) this.random.nextFloat() < probability ? this.replacementBlock : this.normalBlock);
        }
    }
}