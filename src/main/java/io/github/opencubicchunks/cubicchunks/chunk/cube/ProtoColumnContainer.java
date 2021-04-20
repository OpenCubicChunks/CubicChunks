package io.github.opencubicchunks.cubicchunks.chunk.cube;

import net.minecraft.core.IdMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.jetbrains.annotations.Nullable;

public class ProtoColumnContainer extends ChunkBiomeContainer {
    protected ProtoColumnContainer(IdMap<Biome> idMap, LevelHeightAccessor levelHeightAccessor,
                                   Biome[] biomes) {
        super(idMap, levelHeightAccessor, biomes);
    }

    public ProtoColumnContainer(IdMap<Biome> idMap, LevelHeightAccessor levelHeightAccessor, int[] is) {
        super(idMap, levelHeightAccessor, is);
    }

    public ProtoColumnContainer(IdMap<Biome> idMap, LevelHeightAccessor levelHeightAccessor, ChunkPos chunkPos,
                                BiomeSource biomeSource) {
        super(idMap, levelHeightAccessor, chunkPos, biomeSource);
    }

    public ProtoColumnContainer(IdMap<Biome> idMap, LevelHeightAccessor levelHeightAccessor, ChunkPos chunkPos, BiomeSource biomeSource, @Nullable int[] is) {
        super(idMap, levelHeightAccessor, chunkPos, biomeSource, is);
    }
}
