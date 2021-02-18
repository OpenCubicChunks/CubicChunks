package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.StructureSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(NoiseGeneratorSettings.class)
public interface NoiseGeneratorSettingsAccess {
    @Invoker(value = "<init>", remap = false)
    static NoiseGeneratorSettings create(
        StructureSettings structures, NoiseSettings noise, BlockState defaultBlock, BlockState defaultFluid,
        int bedrockRoof, int bedrockFloor, int seaLevel, boolean disableMobGeneration, boolean aquifersEnabled, boolean noiseCavesEnabled, boolean grimstoneEnabled
    ) {
        throw new UnsupportedOperationException();
    }

    @Accessor("disableMobGeneration")
    boolean isDisableMobGeneration();

    @Accessor("aquifersEnabled")
    boolean isAquifersEnabled();

    @Accessor("noiseCavesEnabled")
    boolean isNoiseCavesEnabled();

    @Accessor("grimstoneEnabled")
    boolean isGrimstoneEnabled();
}
