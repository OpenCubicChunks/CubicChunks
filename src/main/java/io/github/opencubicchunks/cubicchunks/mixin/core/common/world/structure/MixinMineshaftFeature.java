package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRandom;
import io.github.opencubicchunks.cubicchunks.world.gen.structure.ICubicStructureFeature;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.MineshaftFeature;
import net.minecraft.world.level.levelgen.feature.configurations.MineshaftConfiguration;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MineshaftFeature.class)
public abstract class MixinMineshaftFeature implements ICubicStructureFeature<MineshaftConfiguration> {

    @Override
    public boolean isFeatureSection(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long worldSeed, WorldgenRandom random, int sectionX, int sectionY, int sectionZ, Biome biome,
                                    SectionPos chunkPos, MineshaftConfiguration config, LevelHeightAccessor levelHeightAccessor) {
        ((CubeWorldGenRandom) random).setLargeFeatureSeed(worldSeed, sectionX, sectionY, sectionZ);
        return random.nextDouble() < config.probability;
    }
}
