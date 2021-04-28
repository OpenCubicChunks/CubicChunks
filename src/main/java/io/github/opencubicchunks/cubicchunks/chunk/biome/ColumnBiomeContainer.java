package io.github.opencubicchunks.cubicchunks.chunk.biome;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.IdMap;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;

public class ColumnBiomeContainer extends ChunkBiomeContainer {
    private static final Biome DUMMY_BIOME = BuiltinRegistries.BIOME.get(Biomes.FOREST.location());

    private Level level;

    public ColumnBiomeContainer(IdMap<Biome> idMap, LevelHeightAccessor heightAccess, @Nullable Level level) {
        super(idMap, heightAccess, new Biome[0]);
        this.level = level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }




    public Biome getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
        if (this.level == null) {
            return DUMMY_BIOME;
        }
        int blockX = biomeX << 2;
        int blockY = biomeY << 2;
        int blockZ = biomeZ << 2;
        IBigCube icube = ((ICubeProvider) level.getChunkSource()).getCube(Coords.blockToCube(blockX), Coords.blockToCube(blockY), Coords.blockToCube(blockZ), ChunkStatus.BIOMES, false);
        if (icube == null) {
            if (!level.isClientSide()) {
                CubicChunks.LOGGER.warn("Tried to get biome at BLOCK pos {} {} {}, but cube isn't loaded. Returning dummy biome", blockX, blockY, blockZ);
            }
            return DUMMY_BIOME;
        }

        ChunkBiomeContainer cubeBiomes = icube.getBiomes();
        if (cubeBiomes != null) {
            return cubeBiomes.getNoiseBiome(biomeX, biomeY, biomeZ);
        }
        CubicChunks.LOGGER.error("Tried to get biome at BLOCK pos {} {} {}, Cube didn't contain a Biome Container", blockX, blockY, blockZ);
        return DUMMY_BIOME;
    }
}
