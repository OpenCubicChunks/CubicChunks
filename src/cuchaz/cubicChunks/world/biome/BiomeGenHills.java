package cuchaz.cubicChunks.world.biome;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeGenHills extends CubeBiomeGenBase
{
    private WorldGenerator theWorldGenerator;
    private WorldGenTaiga2 field_150634_aD;
    private int field_150635_aE;
    private int field_150636_aF;
    private int field_150637_aG;
    private int field_150638_aH;

    protected BiomeGenHills(int biomeID, boolean p_i45373_2_)
    {
        super(biomeID);
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

    public WorldGenAbstractTree checkSpawnTree(Random rand)
    {
        return (WorldGenAbstractTree)(rand.nextInt(3) > 0 ? this.field_150634_aD : super.checkSpawnTree(rand));
    }

    public void decorate(World world, Random rand, int par3, int par4)
    {
        super.decorate(world, rand, par3, par4);
        int var5 = 3 + rand.nextInt(6);
        int var6;
        int var7;
        int var8;

        for (var6 = 0; var6 < var5; ++var6)
        {
            var7 = par3 + rand.nextInt(16);
            var8 = rand.nextInt(28) + 4;
            int var9 = par4 + rand.nextInt(16);

            if (world.getBlock(var7, var8, var9) == Blocks.stone)
            {
                world.setBlock(var7, var8, var9, Blocks.emerald_ore, 0, 2);
            }
        }

        for (var5 = 0; var5 < 7; ++var5)
        {
            var6 = par3 + rand.nextInt(16);
            var7 = rand.nextInt(64);
            var8 = par4 + rand.nextInt(16);
            this.theWorldGenerator.generate(world, rand, var6, var7, var8);
        }
    }

    public void modifyBlocks_pre(World world, Random rand, Block[] blocks, byte[] meta, int xAbs, int yAbs, int zAbs, double var)
    {
        this.topBlock = Blocks.grass;
        this.field_150604_aj = 0;
        this.fillerBlock = Blocks.dirt;

        if ((var < -1.0D || var > 2.0D) && this.field_150638_aH == this.field_150637_aG)
        {
            this.topBlock = Blocks.gravel;
            this.fillerBlock = Blocks.gravel;
        }
        else if (var > 1.0D && this.field_150638_aH != this.field_150636_aF)
        {
            this.topBlock = Blocks.stone;
            this.fillerBlock = Blocks.stone;
        }

        this.modifyBlocks(world, rand, blocks, meta, xAbs, yAbs, zAbs, var);
    }

    private BiomeGenHills func_150633_b(CubeBiomeGenBase biome)
    {
        this.field_150638_aH = this.field_150637_aG;
        this.func_150557_a(biome.color, true);
        this.setBiomeName(biome.biomeName + " M");
        this.setHeightRange(new CubeBiomeGenBase.Height(biome.minHeight, biome.maxHeight));
        this.setTemperatureAndRainfall(biome.temperature, biome.rainfall);
        return this;
    }

    protected CubeBiomeGenBase func_150566_k()
    {
        return (new BiomeGenHills(this.biomeID + 128, false)).func_150633_b(this);
    }
}
