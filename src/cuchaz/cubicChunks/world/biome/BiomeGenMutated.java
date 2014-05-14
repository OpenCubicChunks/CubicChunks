package cuchaz.cubicChunks.world.biome;

import java.util.ArrayList;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;

public class BiomeGenMutated extends BiomeGenBase
{
    protected BiomeGenBase biome;

    public BiomeGenMutated(int biomeID, BiomeGenBase biome)
    {
        super(biomeID);
        this.biome = biome;
        this.func_150557_a(biome.color, true);
        this.biomeName = biome.biomeName + " M";
        this.topBlock = biome.topBlock;
        this.fillerBlock = biome.fillerBlock;
        this.field_76754_C = biome.field_76754_C;
        this.minHeight = biome.minHeight;
        this.maxHeight = biome.maxHeight;
        this.temperature = biome.temperature;
        this.rainfall = biome.rainfall;
        this.waterColorMultiplier = biome.waterColorMultiplier;
        this.enableSnow = biome.enableSnow;
        this.enableRain = biome.enableRain;
        this.spawnableCreatureList = new ArrayList(biome.spawnableCreatureList);
        this.spawnableMonsterList = new ArrayList(biome.spawnableMonsterList);
        this.spawnableCaveCreatureList = new ArrayList(biome.spawnableCaveCreatureList);
        this.spawnableWaterCreatureList = new ArrayList(biome.spawnableWaterCreatureList);
        this.temperature = biome.temperature;
        this.rainfall = biome.rainfall;
        this.minHeight = biome.minHeight + 0.1F;
        this.maxHeight = biome.maxHeight + 0.2F;
    }

    public void decorate(World par1World, Random rand, int par3, int par4)
    {
        this.biome.theBiomeDecorator.func_150512_a(par1World, rand, this, par3, par4);
    }

    public void func_150573_a(World world, Random rand, Block[] blocks, byte[] meta, int xAbs, int zAbs, double p_150573_7_)
    {
        this.biome.func_150573_a(world, rand, blocks, meta, xAbs, zAbs, p_150573_7_);
    }

    /**
     * returns the chance a creature has to spawn.
     */
    public float getSpawningChance()
    {
        return this.biome.getSpawningChance();
    }

    public WorldGenAbstractTree checkSpawnTree(Random p_150567_1_)
    {
        return this.biome.checkSpawnTree(p_150567_1_);
    }

    /**
     * Provides the basic foliage color based on the biome temperature and rainfall
     */
    public int getBiomeFoliageColor(int p_150571_1_, int p_150571_2_, int p_150571_3_)
    {
        return this.biome.getBiomeFoliageColor(p_150571_1_, p_150571_2_, p_150571_2_);
    }

    /**
     * Provides the basic grass color based on the biome temperature and rainfall
     */
    public int getBiomeGrassColor(int p_150558_1_, int p_150558_2_, int p_150558_3_)
    {
        return this.biome.getBiomeGrassColor(p_150558_1_, p_150558_2_, p_150558_2_);
    }

    public Class func_150562_l()
    {
        return this.biome.func_150562_l();
    }

    public boolean func_150569_a(BiomeGenBase biome)
    {
        return this.biome.func_150569_a(biome);
    }

    public BiomeGenBase.TempCategory func_150561_m()
    {
        return this.biome.func_150561_m();
    }
}
