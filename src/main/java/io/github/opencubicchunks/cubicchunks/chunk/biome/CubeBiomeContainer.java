package io.github.opencubicchunks.cubicchunks.chunk.biome;

import io.github.opencubicchunks.cubicchunks.mixin.access.common.BiomeContainerAccess;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.core.SectionPos;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public class CubeBiomeContainer extends ChunkBiomeContainer {

    private static final int SIZE_BITS = (int)Math.round(Math.log(16.0D) / Math.log(2.0D)) - 2;
    public static final int BIOMES_SIZE = 1 << SIZE_BITS + SIZE_BITS + SIZE_BITS;

    private CubeBiomeContainer(IdMap<Biome> intIterable)
    {
        this(intIterable, new Biome[BIOMES_SIZE]);
    }

    public CubeBiomeContainer(IdMap<Biome> indexedIterable, Biome[] biomesIn) {
        super(indexedIterable, biomesIn);
    }


    public CubeBiomeContainer(IdMap<Biome> indexedIterable, SectionPos sectionPosIn, BiomeSource biomeProviderIn) {
        this(indexedIterable);
        int x = sectionPosIn.minBlockX() >> 2;
        int y = sectionPosIn.minBlockY() >> 2;
        int z = sectionPosIn.minBlockZ() >> 2;

        for(int k = 0; k < ((BiomeContainerAccess)this).getBiomes().length; ++k) {
            int dx = k & HORIZONTAL_MASK;
            int dy = k >> SIZE_BITS + SIZE_BITS & HORIZONTAL_MASK;
            int dz = k >> SIZE_BITS & HORIZONTAL_MASK;
            ((BiomeContainerAccess)this).getBiomes()[k] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
        }

    }

    public CubeBiomeContainer(IdMap<Biome> indexedIterable, SectionPos sectionPosIn, BiomeSource biomeProviderIn, @Nullable int[] biomeIds) {
        this(indexedIterable);
        int x = sectionPosIn.minBlockX() >> 2;
        int y = sectionPosIn.minBlockY() >> 2;
        int z = sectionPosIn.minBlockZ() >> 2;
        Biome[] biomes = ((BiomeContainerAccess) this).getBiomes();
        if (biomeIds != null) {
            for(int k = 0; k < biomeIds.length; ++k) {
                //noinspection deprecation
                biomes[k] = BuiltinRegistries.BIOME.byId(biomeIds[k]);
                if (biomes[k] == null) {
                    int dx = k & HORIZONTAL_MASK;
                    int dy = k >> SIZE_BITS + SIZE_BITS & HORIZONTAL_MASK;
                    int dz = k >> SIZE_BITS & HORIZONTAL_MASK;
                    biomes[k] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
                }
            }
        } else {
            for(int k1 = 0; k1 < biomes.length; ++k1) {
                int dx = k1 & HORIZONTAL_MASK;
                int dy = k1 >> SIZE_BITS + SIZE_BITS & HORIZONTAL_MASK;
                int dz = k1 >> SIZE_BITS & HORIZONTAL_MASK;
                biomes[k1] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
            }
        }
    }

    @Override
    public Biome getNoiseBiome(int x, int y, int z) {
        int localX = x & HORIZONTAL_MASK;
        int localY = y & HORIZONTAL_MASK;
        int localZ = z & HORIZONTAL_MASK;
        return ((BiomeContainerAccess)this).getBiomes()[localY << SIZE_BITS + SIZE_BITS | localZ << SIZE_BITS | localX];
    }
}