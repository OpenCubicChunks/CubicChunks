package io.github.opencubicchunks.cubicchunks.chunk.biome;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.IdMap;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;

import javax.annotation.Nullable;

public class CubeBiomeContainer {

    private static final int SIZE_BITS = (int)Math.round(Math.log(IBigCube.DIAMETER_IN_BLOCKS) / Math.log(2.0D)) - 2;
    public static final int CUBE_BIOMES_SIZE = 1 << SIZE_BITS + SIZE_BITS + SIZE_BITS;

    private static final int CUBE_HORIZONTAL_MASK = (1 << SIZE_BITS) - 1;

    private final IdMap<Biome> biomeRegistry;
    private final Biome[] biomes;

    public CubeBiomeContainer(IdMap<Biome> indexedIterable, Biome[] biomesIn) {
        this.biomeRegistry = indexedIterable;
        this.biomes = biomesIn;
    }

    private CubeBiomeContainer(IdMap<Biome> intIterable)
    {
        this(intIterable, new Biome[CUBE_BIOMES_SIZE]);
    }

    private static int n = 0;
    @Environment(EnvType.CLIENT)
    public CubeBiomeContainer(IdMap<Biome> idMap, int[] is) {
        this(idMap);

        for(int i = 0; i < this.biomes.length; ++i) {
            int j = is[i];
            Biome biome = idMap.byId(j);
            if (biome == null) {
                if(n % 10000 == 0)
                    CubicChunks.LOGGER.warn("Received invalid biome id: {}", j);
                n++;
                this.biomes[i] = idMap.byId(0);
            } else {
                this.biomes[i] = biome;
            }
        }
    }

    public CubeBiomeContainer(IdMap<Biome> indexedIterable, CubePos cubePos, BiomeSource biomeProviderIn) {
        this(indexedIterable);
        int biomeNodeShift = MathUtil.log2(IBigCube.DIAMETER_IN_BLOCKS) - 2;
        int x = cubePos.minCubeX() >> biomeNodeShift;
        int y = cubePos.minCubeY() >> biomeNodeShift;
        int z = cubePos.minCubeZ() >> biomeNodeShift;

        for(int k = 0; k < biomes.length; ++k) {
            int dx = k & CUBE_HORIZONTAL_MASK;
            int dy = k >> SIZE_BITS + SIZE_BITS & CUBE_HORIZONTAL_MASK;
            int dz = k >> SIZE_BITS & CUBE_HORIZONTAL_MASK;
            biomes[k] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
        }

    }

    public CubeBiomeContainer(IdMap<Biome> indexedIterable, CubePos cubePos, BiomeSource biomeProviderIn, @Nullable int[] biomeIds) {
        this(indexedIterable);
        int x = cubePos.minCubeX();
        int y = cubePos.minCubeY();
        int z = cubePos.minCubeZ();
        if (biomeIds != null) {
            for(int k = 0; k < biomeIds.length; ++k) {
                biomes[k] = indexedIterable.byId(biomeIds[k]);
                if (biomes[k] == null) {
                    int dx = k & CUBE_HORIZONTAL_MASK;
                    int dy = k >> SIZE_BITS + SIZE_BITS & CUBE_HORIZONTAL_MASK;
                    int dz = k >> SIZE_BITS & CUBE_HORIZONTAL_MASK;
                    biomes[k] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
                }
            }
        } else {
            for(int k1 = 0; k1 < biomes.length; ++k1) {
                int dx = k1 & CUBE_HORIZONTAL_MASK;
                int dy = k1 >> SIZE_BITS + SIZE_BITS & CUBE_HORIZONTAL_MASK;
                int dz = k1 >> SIZE_BITS & CUBE_HORIZONTAL_MASK;
                biomes[k1] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
            }
        }
    }

    public int[] writeBiomes() {
        int[] is = new int[this.biomes.length];

        for(int i = 0; i < this.biomes.length; ++i) {
            is[i] = this.biomeRegistry.getId(this.biomes[i]);
        }

        return is;
    }

    public Biome getNoiseBiome(int x, int y, int z) {
        int localX = x & CUBE_HORIZONTAL_MASK;
        int localY = y & CUBE_HORIZONTAL_MASK;
        int localZ = z & CUBE_HORIZONTAL_MASK;
        return biomes[localY << SIZE_BITS + SIZE_BITS | localZ << SIZE_BITS | localX];
    }
}