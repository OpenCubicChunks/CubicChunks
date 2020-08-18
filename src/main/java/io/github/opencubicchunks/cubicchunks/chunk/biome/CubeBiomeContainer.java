package io.github.opencubicchunks.cubicchunks.chunk.biome;

import io.github.opencubicchunks.cubicchunks.mixin.access.common.BiomeContainerAccess;
import net.minecraft.util.IObjectIntIterable;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.registry.WorldGenRegistries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;

import javax.annotation.Nullable;

public class CubeBiomeContainer extends BiomeContainer {

    private static final int SIZE_BITS = (int)Math.round(Math.log(16.0D) / Math.log(2.0D)) - 2;
    public static final int BIOMES_SIZE = 1 << SIZE_BITS + SIZE_BITS + SIZE_BITS;

    private CubeBiomeContainer(IObjectIntIterable<Biome> intIterable)
    {
        this(intIterable, new Biome[BIOMES_SIZE]);
    }

    public CubeBiomeContainer(IObjectIntIterable<Biome> indexedIterable, Biome[] biomesIn) {
        super(indexedIterable, biomesIn);
    }

//    public CubeBiomeContainer(PacketBuffer packetBufferIn) {
//        super(packetBufferIn);
//    }

    public CubeBiomeContainer(IObjectIntIterable<Biome> indexedIterable, SectionPos sectionPosIn, BiomeProvider biomeProviderIn) {
        this(indexedIterable);
        int x = sectionPosIn.getWorldStartX() >> 2;
        int y = sectionPosIn.getWorldStartY() >> 2;
        int z = sectionPosIn.getWorldStartZ() >> 2;

        for(int k = 0; k < ((BiomeContainerAccess)this).getBiomes().length; ++k) {
            int dx = k & HORIZONTAL_MASK;
            int dy = k >> SIZE_BITS + SIZE_BITS & HORIZONTAL_MASK;
            int dz = k >> SIZE_BITS & HORIZONTAL_MASK;
            ((BiomeContainerAccess)this).getBiomes()[k] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
        }

    }

    public CubeBiomeContainer(IObjectIntIterable<Biome> indexedIterable, SectionPos sectionPosIn, BiomeProvider biomeProviderIn, @Nullable int[] biomeIds) {
        this(indexedIterable);
        int x = sectionPosIn.getWorldStartX() >> 2;
        int y = sectionPosIn.getWorldStartY() >> 2;
        int z = sectionPosIn.getWorldStartZ() >> 2;
        Biome[] biomes = ((BiomeContainerAccess) this).getBiomes();
        if (biomeIds != null) {
            for(int k = 0; k < biomeIds.length; ++k) {
                biomes[k] = WorldGenRegistries.field_243657_i.getByValue(biomeIds[k]);
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
