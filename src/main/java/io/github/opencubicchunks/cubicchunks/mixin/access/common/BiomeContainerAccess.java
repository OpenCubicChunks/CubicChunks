package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.core.IdMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkBiomeContainer.class)
public interface BiomeContainerAccess {
    @Accessor Biome[] getBiomes();

    @Accessor IdMap<Biome> getBiomeRegistry();

    @Accessor("WIDTH_BITS") static int getWidthBits() {
        throw new Error("Mixin did not apply");
    }
}