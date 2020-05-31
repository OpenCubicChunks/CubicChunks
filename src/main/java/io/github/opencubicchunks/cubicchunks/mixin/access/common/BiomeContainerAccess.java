package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeContainer.class)
public interface BiomeContainerAccess {
    @Accessor Biome[] getBiomes();
}
