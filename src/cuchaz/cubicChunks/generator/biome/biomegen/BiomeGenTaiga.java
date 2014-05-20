package cuchaz.cubicChunks.generator.biome.biomegen;

import java.util.Random;

import cuchaz.cubicChunks.generator.CubeBlocks;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.Height;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.SpawnListEntry;
import net.minecraft.block.Block;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBlockBlob;
import net.minecraft.world.gen.feature.WorldGenMegaPineTree;
import net.minecraft.world.gen.feature.WorldGenTaiga1;
import net.minecraft.world.gen.feature.WorldGenTaiga2;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeGenTaiga extends CubeBiomeGenBase
{
    private static final WorldGenTaiga1 field_150639_aC = new WorldGenTaiga1();
    private static final WorldGenTaiga2 field_150640_aD = new WorldGenTaiga2(false);
    private static final WorldGenMegaPineTree field_150641_aE = new WorldGenMegaPineTree(false, false);
    private static final WorldGenMegaPineTree field_150642_aF = new WorldGenMegaPineTree(false, true);
    private static final WorldGenBlockBlob field_150643_aG = new WorldGenBlockBlob(Blocks.mossy_cobblestone, 0);
    private int field_150644_aH;
    private static final String __OBFID = "CL_00000186";

    public BiomeGenTaiga(int p_i45385_1_, int p_i45385_2_)
    {
        super(p_i45385_1_);
        this.field_150644_aH = p_i45385_2_;
        this.spawnableCreatureList.add(new CubeBiomeGenBase.SpawnListEntry(EntityWolf.class, 8, 4, 4));
        this.theBiomeDecorator.treesPerChunk = 10;

        if (p_i45385_2_ != 1 && p_i45385_2_ != 2)
        {
            this.theBiomeDecorator.grassPerChunk = 1;
            this.theBiomeDecorator.mushroomsPerChunk = 1;
        }
        else
        {
            this.theBiomeDecorator.grassPerChunk = 7;
            this.theBiomeDecorator.deadBushPerChunk = 1;
            this.theBiomeDecorator.mushroomsPerChunk = 3;
        }
    }

    public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_)
    {
        return (WorldGenAbstractTree)((this.field_150644_aH == 1 || this.field_150644_aH == 2) && p_150567_1_.nextInt(3) == 0 ? (this.field_150644_aH != 2 && p_150567_1_.nextInt(13) != 0 ? field_150641_aE : field_150642_aF) : (p_150567_1_.nextInt(3) == 0 ? field_150639_aC : field_150640_aD));
    }

    /**
     * Gets a WorldGen appropriate for this biome.
     */
    public WorldGenerator getRandomWorldGenForGrass(Random par1Random)
    {
        return par1Random.nextInt(5) > 0 ? new WorldGenTallGrass(Blocks.tallgrass, 2) : new WorldGenTallGrass(Blocks.tallgrass, 1);
    }

    public void decorate(World par1World, Random par2Random, int par3, int par4)
    {
        int var5;
        int var6;
        int var7;
        int var8;

        if (this.field_150644_aH == 1 || this.field_150644_aH == 2)
        {
            var5 = par2Random.nextInt(3);

            for (var6 = 0; var6 < var5; ++var6)
            {
                var7 = par3 + par2Random.nextInt(16) + 8;
                var8 = par4 + par2Random.nextInt(16) + 8;
                int var9 = par1World.getHeightValue(var7, var8);
                field_150643_aG.generate(par1World, par2Random, var7, var9, var8);
            }
        }

        field_150610_ae.func_150548_a(3);

        for (var5 = 0; var5 < 7; ++var5)
        {
            var6 = par3 + par2Random.nextInt(16) + 8;
            var7 = par4 + par2Random.nextInt(16) + 8;
            var8 = par2Random.nextInt(par1World.getHeightValue(var6, var7) + 32);
            field_150610_ae.generate(par1World, par2Random, var6, var8, var7);
        }

        super.decorate(par1World, par2Random, par3, par4);
    }

    public void modifyBlocks_pre(World world, Random rand, CubeBlocks cubeBlocks, int xAbs, int yAbs, int zAbs, double val)
    {
        if (this.field_150644_aH == 1 || this.field_150644_aH == 2)
        {
            this.topBlock = Blocks.grass;
            this.field_150604_aj = 0;
            this.fillerBlock = Blocks.dirt;

            if (val > 1.75D)
            {
                this.topBlock = Blocks.dirt;
                this.field_150604_aj = 1;
            }
            else if (val > -0.95D)
            {
                this.topBlock = Blocks.dirt;
                this.field_150604_aj = 2;
            }
        }

        this.modifyBlocks(world, rand, cubeBlocks, xAbs, yAbs, zAbs, val);
    }

    protected CubeBiomeGenBase func_150566_k()
    {
        return this.biomeID == CubeBiomeGenBase.megaTaiga.biomeID ? (new BiomeGenTaiga(this.biomeID + 128, 2)).func_150557_a(5858897, true).setBiomeName("Mega Spruce Taiga").func_76733_a(5159473).setTemperatureAndRainfall(0.25F, 0.8F).setHeightRange(new CubeBiomeGenBase.Height(this.minHeight, this.maxHeight)) : super.func_150566_k();
    }
}
