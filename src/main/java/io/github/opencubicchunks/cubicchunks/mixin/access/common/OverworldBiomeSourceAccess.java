package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.OverworldBiomeSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverworldBiomeSource.class)
public interface OverworldBiomeSourceAccess {

    @Accessor
    Registry<Biome> getBiomes();

}
