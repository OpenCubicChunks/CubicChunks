package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.chunk.NonAtomicWorldgenRandom;
import net.minecraft.world.level.levelgen.RandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldgenRandom.class)
public class MixinWorldGenRandom extends Random implements RandomSource {

    private long seed;

    @Inject(method = "<init>(J)V", at = @At("RETURN"))
    private void noAtomicSeed(long seed, CallbackInfo ci) {
        this.seed = NonAtomicWorldgenRandom.scramble(seed);
    }

    public MixinWorldGenRandom() {
        super(0L);
    }

    @Override public void setSeed(long seed) {
        this.seed = NonAtomicWorldgenRandom.scramble(seed);
    }

    @Override
    public int next(int bits) {
        long nextSeed = NonAtomicWorldgenRandom.nextSeed(this.seed);
        this.seed = nextSeed;
        return NonAtomicWorldgenRandom.sample(nextSeed, bits);
    }
}
