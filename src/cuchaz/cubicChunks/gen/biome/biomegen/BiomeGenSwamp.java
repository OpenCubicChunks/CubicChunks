package cuchaz.cubicChunks.gen.biome.biomegen;

import java.util.Random;

import cuchaz.cubicChunks.gen.CubeBlocks;
import cuchaz.cubicChunks.gen.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

public class BiomeGenSwamp extends CubeBiomeGenBase
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
        this.spawnableMonsterList.add(new CubeBiomeGenBase.SpawnListEntry(EntitySlime.class, 1, 1, 1));
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

    public void modifyBlocks_pre(World world, Random rand, CubeBlocks cubeBlocks, int xAbs, int yAbs, int zAbs, double val)
    {
        double var9 = field_150606_ad.func_151601_a((double)xAbs * 0.25D, (double)yAbs * 0.25D);

        if (var9 > 0.0D)
        {
            int xRel = xAbs & 15;
            int yRel = yAbs & 15;
            int zRel = zAbs & 15;
//            int height = blocks.length / 256;

            for (int y = 16; y >= 0; --y)
            {
                int loc = (zRel * 16 + xRel) * 16 + yRel;

                Block block = cubeBlocks.getBlock(xRel, yRel, zRel);
                
                if (block == null || block.getMaterial() != Material.air)
                {
                    if (yAbs == 62 && block != Blocks.water)
                    {
                        cubeBlocks.setBlock(xRel, yRel, zRel, Blocks.water);

                        if (var9 < 0.12D)
                        {
                        	cubeBlocks.setBlock(xRel, yRel + 1, zRel, Blocks.waterlily); // this should always place the lily at a height of 63, 
                            									//and not go into the next cube up which would be bad.
                        }
                    }

                    break;
                }
            }
        }

        this.modifyBlocks(world, rand, cubeBlocks, xAbs, yAbs, zAbs, val);
    }
}
