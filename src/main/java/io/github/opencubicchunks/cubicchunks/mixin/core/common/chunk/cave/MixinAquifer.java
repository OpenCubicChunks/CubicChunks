package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.cave;

import io.github.opencubicchunks.cubicchunks.chunk.AquiferRandom;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Aquifer.class)
public abstract class MixinAquifer {
    private final AquiferRandom random = new AquiferRandom();

    // optimization: don't create a new random instance every time
    @Redirect(method = "computeAt", at = @At(value = "NEW", target = "net/minecraft/world/level/levelgen/WorldgenRandom"))
    private WorldgenRandom createRandom(long seed) {
        AquiferRandom random = this.random;
        random.setSeed(seed);
        return random;
    }
}
