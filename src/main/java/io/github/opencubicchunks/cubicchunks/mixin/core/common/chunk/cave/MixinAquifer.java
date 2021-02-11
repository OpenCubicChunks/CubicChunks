package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave;

import java.util.Arrays;

import io.github.opencubicchunks.cubicchunks.chunk.AquiferRandom;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeAquifer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSampler;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Aquifer.class)
public abstract class MixinAquifer implements ICubeAquifer {
    @Mutable @Shadow @Final private int minGridY;

    @Shadow protected abstract int gridY(int i);

    @Mutable @Shadow @Final private int[] aquiferCache;

    @Shadow @Final private int gridSizeX;

    @Shadow @Final private int gridSizeZ;

    @Mutable @Shadow @Final private long[] aquiferLocationCache;

    private final AquiferRandom random = new AquiferRandom();

    private int yGridSize;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void collectYSize(int i, int j, NormalNoise normalNoise, NormalNoise normalNoise2, NoiseGeneratorSettings noiseGeneratorSettings, NoiseSampler noiseSampler, int yGridSize,
                              CallbackInfo ci) {
        this.yGridSize = yGridSize;
    }

    @Override public void prepareLocalWaterLevelForCube(ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        this.minGridY = this.gridY(minY) - 1;
        int maxGridY = this.gridY(minY + yGridSize) + 1;
        int gridSizeY = maxGridY - this.minGridY + 1;
        int capacity = this.gridSizeX * gridSizeY * this.gridSizeZ;
        this.aquiferCache = new int[capacity];
        Arrays.fill(this.aquiferCache, Integer.MAX_VALUE);
        this.aquiferLocationCache = new long[capacity];
        Arrays.fill(this.aquiferLocationCache, Long.MAX_VALUE);
    }

    // optimization: don't create a new random instance every time
    @Redirect(method = "computeAt", at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/WorldgenRandom"))
    private WorldgenRandom createRandom(long seed) {
        AquiferRandom random = this.random;
        random.setSeed(seed);
        return random;
    }
}
