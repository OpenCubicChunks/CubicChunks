package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NoiseGeneratorSettings.class)
public interface NoiseGeneratorSettingsAccess {

    @Accessor("disableMobGeneration")
    boolean isDisableMobGeneration();

    @Accessor("aquifersEnabled")
    boolean isAquifersEnabled();

    @Accessor("noiseCavesEnabled")
    boolean isNoiseCavesEnabled();

    @Accessor("deepslateEnabled")
    boolean isDeepSlateEnabled();
}
