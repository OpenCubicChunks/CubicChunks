package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.biome.BiomeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeManager.class)
public interface BiomeManagerAccess {

    @Accessor static int getCHUNK_CENTER_QUART() {
        throw new Error("Mixin did not apply");
    }
}