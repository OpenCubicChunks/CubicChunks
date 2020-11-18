package io.github.opencubicchunks.cubicchunks.chunk.biome;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.IdMap;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class ColumnBiomeContainer extends ChunkBiomeContainer {
    private static final Biome DUMMY_BIOME = BuiltinRegistries.BIOME.get(Biomes.FOREST.location());

    private ChunkSource chunkSource;

    public ColumnBiomeContainer(IdMap<Biome> idMap, Biome[] biomes) {
        super(idMap, biomes);
    }

    public ColumnBiomeContainer(IdMap<Biome> idMap, int[] is) {
        super(idMap, is);
    }

    public ColumnBiomeContainer(IdMap<Biome> idMap, ChunkPos chunkPos, BiomeSource biomeSource) {
        super(idMap, chunkPos, biomeSource);
    }

    public ColumnBiomeContainer(IdMap<Biome> idMap, ChunkPos chunkPos, BiomeSource biomeSource, @Nullable int[] is) {
        super(idMap, chunkPos, biomeSource, is);
    }

    public void setChunkSource(ChunkSource chunkSource) {
        this.chunkSource = chunkSource;
    }

    public Biome getNoiseBiome(int x, int y, int z) {
        if(this.chunkSource == null) {
            return DUMMY_BIOME;
        }
        IBigCube icube = ((ICubeProvider) chunkSource).getCube(Coords.blockToCube(x), Coords.blockToCube(y), Coords.blockToCube(z), ChunkStatus.BIOMES, true);
        CubeBiomeContainer cubeBiomes = icube.getCubeBiomes();
        if(cubeBiomes != null) {
            return cubeBiomes.getNoiseBiome(x, y, z);
        }
        CubicChunks.LOGGER.warn("Tried to get biome at pos {} {} {}, but cube isn't loaded. Returning dummy biome", x, y, z);
        return DUMMY_BIOME;
    }
}
