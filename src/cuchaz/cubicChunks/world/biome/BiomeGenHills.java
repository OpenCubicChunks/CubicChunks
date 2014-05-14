package cuchaz.cubicChunks.world.biome;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeGenHills extends BiomeGenBase
{
    private WorldGenerator theWorldGenerator;
    private WorldGenTaiga2 field_150634_aD;
    private int field_150635_aE;
    private int field_150636_aF;
    private int field_150637_aG;
    private int field_150638_aH;

    protected BiomeGenHills(int p_i45373_1_, boolean p_i45373_2_)
    {
        super(p_i45373_1_);
        this.theWorldGenerator = new WorldGenMinable(Blocks.monster_egg, 8);
        this.field_150634_aD = new WorldGenTaiga2(false);
        this.field_150635_aE = 0;
        this.field_150636_aF = 1;
        this.field_150637_aG = 2;
        this.field_150638_aH = this.field_150635_aE;

        if (p_i45373_2_)
        {
            this.theBiomeDecorator.treesPerChunk = 3;
            this.field_150638_aH = this.field_150636_aF;
        }
    }

    public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_)
    {
        return (WorldGenAbstractTree)(p_150567_1_.nextInt(3) > 0 ? this.field_150634_aD : super.checkSpawnTree(p_150567_1_));
    }

    public void decorate(World par1World, Random par2Random, int par3, int par4)
    {
        super.decorate(par1World, par2Random, par3, par4);
        int var5 = 3 + par2Random.nextInt(6);
        int var6;
        int var7;
        int var8;

        for (var6 = 0; var6 < var5; ++var6)
        {
            var7 = par3 + par2Random.nextInt(16);
            var8 = par2Random.nextInt(28) + 4;
            int var9 = par4 + par2Random.nextInt(16);

            if (par1World.getBlock(var7, var8, var9) == Blocks.stone)
            {
                par1World.setBlock(var7, var8, var9, Blocks.emerald_ore, 0, 2);
            }
        }

        for (var5 = 0; var5 < 7; ++var5)
        {
            var6 = par3 + par2Random.nextInt(16);
            var7 = par2Random.nextInt(64);
            var8 = par4 + par2Random.nextInt(16);
            this.theWorldGenerator.generate(par1World, par2Random, var6, var7, var8);
        }
    }

    public void func_150573_a(World p_150573_1_, Random p_150573_2_, Block[] p_150573_3_, byte[] p_150573_4_, int p_150573_5_, int p_150573_6_, double p_150573_7_)
    {
        this.topBlock = Blocks.grass;
        this.field_150604_aj = 0;
        this.fillerBlock = Blocks.dirt;

        if ((p_150573_7_ < -1.0D || p_150573_7_ > 2.0D) && this.field_150638_aH == this.field_150637_aG)
        {
            this.topBlock = Blocks.gravel;
            this.fillerBlock = Blocks.gravel;
        }
        else if (p_150573_7_ > 1.0D && this.field_150638_aH != this.field_150636_aF)
        {
            this.topBlock = Blocks.stone;
            this.fillerBlock = Blocks.stone;
        }

        this.modifyBlocks(p_150573_1_, p_150573_2_, p_150573_3_, p_150573_4_, p_150573_5_, p_150573_6_, p_150573_7_);
    }

    private BiomeGenHills func_150633_b(BiomeGenBase p_150633_1_)
    {
        this.field_150638_aH = this.field_150637_aG;
        this.func_150557_a(p_150633_1_.color, true);
        this.setBiomeName(p_150633_1_.biomeName + " M");
        this.setHeightRange(new BiomeGenBase.Height(p_150633_1_.minHeight, p_150633_1_.maxHeight));
        this.setTemperatureAndRainfall(p_150633_1_.temperature, p_150633_1_.rainfall);
        return this;
    }

    protected BiomeGenBase func_150566_k()
    {
        return (new BiomeGenHills(this.biomeID + 128, false)).func_150633_b(this);
    }
}
