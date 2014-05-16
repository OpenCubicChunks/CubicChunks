package cuchaz.cubicChunks.world.biome;

import java.util.Random;

import cuchaz.cubicChunks.gen.CubeBlocks;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenSavannaTree;

public class BiomeGenSavanna extends CubeBiomeGenBase
{
    private static final WorldGenSavannaTree field_150627_aC = new WorldGenSavannaTree(false);
    private static final String __OBFID = "CL_00000182";

    protected BiomeGenSavanna(int p_i45383_1_)
    {
        super(p_i45383_1_);
        this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityHorse.class, 1, 2, 6));
        this.theBiomeDecorator.treesPerChunk = 1;
        this.theBiomeDecorator.flowersPerChunk = 4;
        this.theBiomeDecorator.grassPerChunk = 20;
    }

    public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_)
    {
        return (WorldGenAbstractTree)(p_150567_1_.nextInt(5) > 0 ? field_150627_aC : this.worldGeneratorTrees);
    }

    protected CubeBiomeGenBase func_150566_k()
    {
        BiomeGenSavanna.Mutated var1 = new BiomeGenSavanna.Mutated(this.biomeID + 128, this);
        var1.temperature = (this.temperature + 1.0F) * 0.5F;
        var1.minHeight = this.minHeight * 0.5F + 0.3F;
        var1.maxHeight = this.maxHeight * 0.5F + 1.2F;
        return var1;
    }

    public void decorate(World par1World, Random par2Random, int par3, int par4)
    {
        field_150610_ae.func_150548_a(2);

        for (int var5 = 0; var5 < 7; ++var5)
        {
            int var6 = par3 + par2Random.nextInt(16) + 8;
            int var7 = par4 + par2Random.nextInt(16) + 8;
            int var8 = par2Random.nextInt(par1World.getHeightValue(var6, var7) + 32);
            field_150610_ae.generate(par1World, par2Random, var6, var8, var7);
        }

        super.decorate(par1World, par2Random, par3, par4);
    }

    public static class Mutated extends BiomeGenMutated
    {
        private static final String __OBFID = "CL_00000183";

        public Mutated(int p_i45382_1_, CubeBiomeGenBase p_i45382_2_)
        {
            super(p_i45382_1_, p_i45382_2_);
            this.theBiomeDecorator.treesPerChunk = 2;
            this.theBiomeDecorator.flowersPerChunk = 2;
            this.theBiomeDecorator.grassPerChunk = 5;
        }

        public void modifyBlocks_pre(World world, Random rand, CubeBlocks cubeBlocks, int xAbs, int yAbs, int zAbs, double val)
        {
            this.topBlock = Blocks.grass;
            this.field_150604_aj = 0;
            this.fillerBlock = Blocks.dirt;

            if (val > 1.75D)
            {
                this.topBlock = Blocks.stone;
                this.fillerBlock = Blocks.stone;
            }
            else if (val > -0.5D)
            {
                this.topBlock = Blocks.dirt;
                this.field_150604_aj = 1;
            }

            this.modifyBlocks(world, rand, cubeBlocks, xAbs, yAbs, zAbs, val);
        }

        public void decorate(World par1World, Random par2Random, int par3, int par4)
        {
            this.theBiomeDecorator.func_150512_a(par1World, par2Random, this, par3, par4);
        }
    }
}
