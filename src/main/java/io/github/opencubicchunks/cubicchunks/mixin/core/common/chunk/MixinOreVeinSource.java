package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.NonAtomicWorldgenRandom;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@SuppressWarnings("UnresolvedMixinReference")
@Mixin(targets = "net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator$OreVeinNoiseSource")
public class MixinOreVeinSource {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/WorldgenRandom"))
    private WorldgenRandom createOreVeinRandom() {
        return new NonAtomicWorldgenRandom();
    }
}
