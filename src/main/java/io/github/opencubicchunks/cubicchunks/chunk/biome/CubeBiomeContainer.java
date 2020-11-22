package io.github.opencubicchunks.cubicchunks.chunk.biome;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

public class CubeBiomeContainer {

    private static final int SIZE_BITS = (int) Math.round(Math.log(IBigCube.DIAMETER_IN_BLOCKS) / Math.log(2.0D)) - 2;
    public static final int CUBE_BIOMES_SIZE = 1 << SIZE_BITS + SIZE_BITS + SIZE_BITS;

    private static final int CUBE_HORIZONTAL_MASK = (1 << SIZE_BITS) - 1;

    private final IdMap<Biome> biomeRegistry;
    private final Biome[] biomes;

    @Environment(EnvType.CLIENT)
    public CubeBiomeContainer(IdMap<Biome> idMap, int[] is) {
        this.biomeRegistry = idMap;
        if (is.length == 1) {
            this.biomes = new Biome[1];
        } else {
            this.biomes = new Biome[CUBE_BIOMES_SIZE];
        }


        for (int i = 0; i < this.biomes.length; ++i) {
            int j = is[i];
            Biome biome = idMap.byId(j);
            if (biome == null) {
                CubicChunks.LOGGER.warn("Received invalid biome id: {}", j);
                this.biomes[i] = idMap.byId(0);
            } else {
                this.biomes[i] = biome;
            }
        }
    }

    public CubeBiomeContainer(IdMap<Biome> idMap, CubePos cubePos, BiomeSource biomeProviderIn) {
        this.biomeRegistry = idMap;
        Biome[] tempBiomes = new Biome[CUBE_BIOMES_SIZE];

        int x = cubePos.minCubeX() >> 2;
        int y = cubePos.minCubeY() >> 2;
        int z = cubePos.minCubeZ() >> 2;

        boolean allBiomesIdentical = true;
        Biome firstBiome = biomeProviderIn.getNoiseBiome(x, y, z);
        for (int k = 0; k < tempBiomes.length; ++k) {
            int dx = k & CUBE_HORIZONTAL_MASK;
            int dy = k >> SIZE_BITS + SIZE_BITS & CUBE_HORIZONTAL_MASK;
            int dz = k >> SIZE_BITS & CUBE_HORIZONTAL_MASK;
            Biome biome = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
            if (biome != firstBiome) {
                allBiomesIdentical = false;
            }
            tempBiomes[k] = biome;
        }

        if (allBiomesIdentical) {
            this.biomes = new Biome[] { firstBiome };
        } else {
            this.biomes = tempBiomes;
        }
    }

    public CubeBiomeContainer(IdMap<Biome> idMap, CubePos cubePos, BiomeSource biomeProviderIn, @Nullable int[] biomeIds) {
        this.biomeRegistry = idMap;
        Biome[] tempBiomes = new Biome[CUBE_BIOMES_SIZE];

        int x = cubePos.minCubeX() >> 2;
        int y = cubePos.minCubeY() >> 2;
        int z = cubePos.minCubeZ() >> 2;

        boolean allBiomesIdentical = true;
        Biome firstBiome = biomeProviderIn.getNoiseBiome(x, y, z);
        if (biomeIds != null) {
            for (int k = 0; k < biomeIds.length; ++k) {
                tempBiomes[k] = idMap.byId(biomeIds[k]);
                if (tempBiomes[k] == null) {
                    int dx = k & CUBE_HORIZONTAL_MASK;
                    int dy = k >> SIZE_BITS + SIZE_BITS & CUBE_HORIZONTAL_MASK;
                    int dz = k >> SIZE_BITS & CUBE_HORIZONTAL_MASK;
                    Biome biome = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
                    if (biome != firstBiome) {
                        allBiomesIdentical = false;
                    }
                    tempBiomes[k] = biome;
                }
            }
        } else {
            for (int k1 = 0; k1 < tempBiomes.length; ++k1) {
                int dx = k1 & CUBE_HORIZONTAL_MASK;
                int dy = k1 >> SIZE_BITS + SIZE_BITS & CUBE_HORIZONTAL_MASK;
                int dz = k1 >> SIZE_BITS & CUBE_HORIZONTAL_MASK;
                Biome biome = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
                if (biome != firstBiome) {
                    allBiomesIdentical = false;
                }
                tempBiomes[k1] = biome;
            }
        }

        if (allBiomesIdentical) {
            this.biomes = new Biome[] { firstBiome };
        } else {
            this.biomes = tempBiomes;
        }
    }

    public int[] writeBiomes() {
        int[] is = new int[this.biomes.length];

        for (int i = 0; i < this.biomes.length; ++i) {
            is[i] = this.biomeRegistry.getId(this.biomes[i]);
        }
        return is;
    }

    public Biome getNoiseBiome(int x, int y, int z) {
        if (biomes.length == 1) {
            return biomes[0];
        }

        int localX = x & CUBE_HORIZONTAL_MASK;
        int localY = y & CUBE_HORIZONTAL_MASK;
        int localZ = z & CUBE_HORIZONTAL_MASK;
        return biomes[localY << SIZE_BITS + SIZE_BITS | localZ << SIZE_BITS | localX];
    }
}