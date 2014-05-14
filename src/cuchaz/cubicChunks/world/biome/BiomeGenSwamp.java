package cuchaz.cubicChunks.world.biome;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

public class BiomeGenSwamp extends BiomeGenBase
{
    private static final String __OBFID = "CL_00000185";

    protected BiomeGenSwamp(int par1)
    {
        super(par1);
        this.theBiomeDecorator.treesPerChunk = 2;
        this.theBiomeDecorator.flowersPerChunk = 1;
        this.theBiomeDecorator.deadBushPerChunk = 1;
        this.theBiomeDecorator.mushroomsPerChunk = 8;
        this.theBiomeDecorator.reedsPerChunk = 10;
        this.theBiomeDecorator.clayPerChunk = 1;
        this.theBiomeDecorator.waterlilyPerChunk = 4;
        this.theBiomeDecorator.sandPerChunk2 = 0;
        this.theBiomeDecorator.sandPerChunk = 0;
        this.theBiomeDecorator.grassPerChunk = 5;
        this.waterColorMultiplier = 14745518;
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntitySlime.class, 1, 1, 1));
    }

    public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_)
    {
        return this.worldGeneratorSwamp;
    }

    /**
     * Provides the basic grass color based on the biome temperature and rainfall
     */
    public int getBiomeGrassColor(int p_150558_1_, int p_150558_2_, int p_150558_3_)
    {
        double var4 = field_150606_ad.func_151601_a((double)p_150558_1_ * 0.0225D, (double)p_150558_3_ * 0.0225D);
        return var4 < -0.1D ? 5011004 : 6975545;
    }

    /**
     * Provides the basic foliage color based on the biome temperature and rainfall
     */
    public int getBiomeFoliageColor(int p_150571_1_, int p_150571_2_, int p_150571_3_)
    {
        return 6975545;
    }

    public String spawnFlower(Random p_150572_1_, int p_150572_2_, int p_150572_3_, int p_150572_4_)
    {
        return BlockFlower.field_149859_a[1];
    }

    public void func_150573_a(World p_150573_1_, Random p_150573_2_, Block[] p_150573_3_, byte[] p_150573_4_, int p_150573_5_, int p_150573_6_, double p_150573_7_)
    {
        double var9 = field_150606_ad.func_151601_a((double)p_150573_5_ * 0.25D, (double)p_150573_6_ * 0.25D);

        if (var9 > 0.0D)
        {
            int var11 = p_150573_5_ & 15;
            int var12 = p_150573_6_ & 15;
            int var13 = p_150573_3_.length / 256;

            for (int var14 = 255; var14 >= 0; --var14)
            {
                int var15 = (var12 * 16 + var11) * var13 + var14;

                if (p_150573_3_[var15] == null || p_150573_3_[var15].getMaterial() != Material.air)
                {
                    if (var14 == 62 && p_150573_3_[var15] != Blocks.water)
                    {
                        p_150573_3_[var15] = Blocks.water;

                        if (var9 < 0.12D)
                        {
                            p_150573_3_[var15 + 1] = Blocks.waterlily;
                        }
                    }

                    break;
                }
            }
        }

        this.modifyBlocks(p_150573_1_, p_150573_2_, p_150573_3_, p_150573_4_, p_150573_5_, p_150573_6_, p_150573_7_);
    }
}
