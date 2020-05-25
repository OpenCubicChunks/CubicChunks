package cubicchunks.cc.chunk.biome;

import cubicchunks.cc.mixin.core.common.biome.IBiomeContainer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

public class SectionBiomeContainer extends BiomeContainer {

    private static final int SIZE_BITS = (int)Math.round(Math.log(16.0D) / Math.log(2.0D)) - 2;
    public static final int BIOMES_SIZE = 1 << SIZE_BITS + SIZE_BITS + SIZE_BITS;

    private SectionBiomeContainer()
    {
        this(new Biome[BIOMES_SIZE]);
    }

    public SectionBiomeContainer(Biome[] biomesIn) {
        super(biomesIn);
    }

    public SectionBiomeContainer(PacketBuffer packetBufferIn) {
        super(packetBufferIn);
    }

    public SectionBiomeContainer(SectionPos sectionPosIn, BiomeProvider biomeProviderIn) {
        this();
        int x = sectionPosIn.getWorldStartX() >> 2;
        int y = sectionPosIn.getWorldStartY() >> 2;
        int z = sectionPosIn.getWorldStartZ() >> 2;

        for(int k = 0; k < ((IBiomeContainer)this).getBiomes().length; ++k) {
            int dx = k & HORIZONTAL_MASK;
            int dy = k >> SIZE_BITS + SIZE_BITS & HORIZONTAL_MASK;
            int dz = k >> SIZE_BITS & HORIZONTAL_MASK;
            ((IBiomeContainer)this).getBiomes()[k] = biomeProviderIn.getNoiseBiome(x + dx, y + dy, z + dz);
        }

    }

    public SectionBiomeContainer(SectionPos sectionPosIn, BiomeProvider biomeProviderIn, @Nullable int[] biomeIds) {
        this();
        int x = sectionPosIn.getWorldStartX() >> 2;
        int y = sectionPosIn.getWorldStartY() >> 2;
        int z = sectionPosIn.getWorldStartZ() >> 2;
        Biome[] biomes = ((IBiomeContainer) this).getBiomes();
        if (biomeIds != null) {
            for(int k = 0; k < biomeIds.length; ++k) {
                biomes[k] = Registry.BIOME.getByValue(biomeIds[k]);
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
        return ((IBiomeContainer)this).getBiomes()[localY << SIZE_BITS + SIZE_BITS | localZ << SIZE_BITS | localX];
    }
}