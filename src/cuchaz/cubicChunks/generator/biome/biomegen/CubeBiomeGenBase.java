/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package cuchaz.cubicChunks.generator.biome.biomegen;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySheep;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.ColorizerGrass;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigTree;
import net.minecraft.world.gen.feature.WorldGenDoublePlant;
import net.minecraft.world.gen.feature.WorldGenSwamp;
import net.minecraft.world.gen.feature.WorldGenTallGrass;
import net.minecraft.world.gen.feature.WorldGenTrees;
import net.minecraft.world.gen.feature.WorldGenerator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;

import cuchaz.cubicChunks.world.Cube;
import cuchaz.magicMojoModLoader.api.events.BuildSizeEvent;

public abstract class CubeBiomeGenBase extends net.minecraft.world.biome.BiomeGenBase
{
    private static final Logger logger = LogManager.getLogger();
    protected static final CubeBiomeGenBase.Height field_150596_a = new CubeBiomeGenBase.Height(0.1F, 0.2F);
    protected static final CubeBiomeGenBase.Height field_150594_b = new CubeBiomeGenBase.Height(-0.5F, 0.0F);
    protected static final CubeBiomeGenBase.Height oceanRange = new CubeBiomeGenBase.Height(-1.0F, 0.1F);
    protected static final CubeBiomeGenBase.Height deepOceanRange = new CubeBiomeGenBase.Height(-1.8F, 0.1F);
    protected static final CubeBiomeGenBase.Height field_150593_e = new CubeBiomeGenBase.Height(0.125F, 0.05F);
    protected static final CubeBiomeGenBase.Height field_150590_f = new CubeBiomeGenBase.Height(0.2F, 0.2F);
    protected static final CubeBiomeGenBase.Height field_150591_g = new CubeBiomeGenBase.Height(0.45F, 0.3F);
    protected static final CubeBiomeGenBase.Height field_150602_h = new CubeBiomeGenBase.Height(1.5F, 0.025F);
    protected static final CubeBiomeGenBase.Height field_150603_i = new CubeBiomeGenBase.Height(1.0F, 0.5F);
    protected static final CubeBiomeGenBase.Height field_150600_j = new CubeBiomeGenBase.Height(0.0F, 0.025F);
    protected static final CubeBiomeGenBase.Height field_150601_k = new CubeBiomeGenBase.Height(0.1F, 0.8F);
    protected static final CubeBiomeGenBase.Height field_150598_l = new CubeBiomeGenBase.Height(0.2F, 0.3F);
    protected static final CubeBiomeGenBase.Height swampRange = new CubeBiomeGenBase.Height(-0.2F, 0.1F);

    /** An array of all the biomes, indexed by biome id. */
    private static final CubeBiomeGenBase[] biomeList = new CubeBiomeGenBase[256];
    
    public static final Set<CubeBiomeGenBase> field_150597_n = Sets.newHashSet();
    
    public static final CubeBiomeGenBase ocean = (new BiomeGenOcean(0)).setColor(112).setBiomeName("Ocean").setHeightRange(oceanRange);
    public static final CubeBiomeGenBase plains = (new BiomeGenPlains(1)).setColor(9286496).setBiomeName("Plains");
    public static final CubeBiomeGenBase desert = (new BiomeGenDesert(2)).setColor(16421912).setBiomeName("Desert").setDisableRain().setTemperatureAndRainfall(2.0F, 0.0F).setHeightRange(field_150593_e);
    public static final CubeBiomeGenBase extremeHills = (new BiomeGenHills(3, false)).setColor(6316128).setBiomeName("Extreme Hills").setHeightRange(field_150603_i).setTemperatureAndRainfall(0.2F, 0.3F);
    public static final CubeBiomeGenBase forest = (new BiomeGenForest(4, 0)).setColor(353825).setBiomeName("Forest");
    public static final CubeBiomeGenBase taiga = (new BiomeGenTaiga(5, 0)).setColor(747097).setBiomeName("Taiga").func_76733_a(5159473).setTemperatureAndRainfall(0.25F, 0.8F).setHeightRange(field_150590_f);
    public static final CubeBiomeGenBase swampland = (new BiomeGenSwamp(6)).setColor(522674).setBiomeName("Swampland").func_76733_a(9154376).setHeightRange(swampRange).setTemperatureAndRainfall(0.8F, 0.9F);
    public static final CubeBiomeGenBase river = (new BiomeGenRiver(7)).setColor(255).setBiomeName("River").setHeightRange(field_150594_b);
    public static final CubeBiomeGenBase hell = (new BiomeGenHell(8)).setColor(16711680).setBiomeName("Hell").setDisableRain().setTemperatureAndRainfall(2.0F, 0.0F);

    /** Is the biome used for sky world. */
    public static final CubeBiomeGenBase sky = (new BiomeGenEnd(9)).setColor(8421631).setBiomeName("Sky").setDisableRain();
    
    public static final CubeBiomeGenBase frozenOcean = (new BiomeGenOcean(10)).setColor(9474208).setBiomeName("FrozenOcean").setEnableSnow().setHeightRange(oceanRange).setTemperatureAndRainfall(0.0F, 0.5F);
    public static final CubeBiomeGenBase frozenRiver = (new BiomeGenRiver(11)).setColor(10526975).setBiomeName("FrozenRiver").setEnableSnow().setHeightRange(field_150594_b).setTemperatureAndRainfall(0.0F, 0.5F);
    public static final CubeBiomeGenBase icePlains = (new BiomeGenSnow(12, false)).setColor(16777215).setBiomeName("Ice Plains").setEnableSnow().setTemperatureAndRainfall(0.0F, 0.5F).setHeightRange(field_150593_e);
    public static final CubeBiomeGenBase iceMountains = (new BiomeGenSnow(13, false)).setColor(10526880).setBiomeName("Ice Mountains").setEnableSnow().setHeightRange(field_150591_g).setTemperatureAndRainfall(0.0F, 0.5F);
    public static final CubeBiomeGenBase mushroomIsland = (new BiomeGenMushroomIsland(14)).setColor(16711935).setBiomeName("MushroomIsland").setTemperatureAndRainfall(0.9F, 1.0F).setHeightRange(field_150598_l);
    public static final CubeBiomeGenBase mushroomIslandShore = (new BiomeGenMushroomIsland(15)).setColor(10486015).setBiomeName("MushroomIslandShore").setTemperatureAndRainfall(0.9F, 1.0F).setHeightRange(field_150600_j);

    /** Beach biome. */
    public static final CubeBiomeGenBase beach = (new BiomeGenBeach(16)).setColor(16440917).setBiomeName("Beach").setTemperatureAndRainfall(0.8F, 0.4F).setHeightRange(field_150600_j);

    /** Desert Hills biome. */
    public static final CubeBiomeGenBase desertHills = (new BiomeGenDesert(17)).setColor(13786898).setBiomeName("DesertHills").setDisableRain().setTemperatureAndRainfall(2.0F, 0.0F).setHeightRange(field_150591_g);

    /** Forest Hills biome. */
    public static final CubeBiomeGenBase forestHills = (new BiomeGenForest(18, 0)).setColor(2250012).setBiomeName("ForestHills").setHeightRange(field_150591_g);

    /** Taiga Hills biome. */
    public static final CubeBiomeGenBase taigaHills = (new BiomeGenTaiga(19, 0)).setColor(1456435).setBiomeName("TaigaHills").func_76733_a(5159473).setTemperatureAndRainfall(0.25F, 0.8F).setHeightRange(field_150591_g);

    /** Extreme Hills Edge biome. */
    public static final CubeBiomeGenBase extremeHillsEdge = (new BiomeGenHills(20, true)).setColor(7501978).setBiomeName("Extreme Hills Edge").setHeightRange(field_150603_i.func_150775_a()).setTemperatureAndRainfall(0.2F, 0.3F);

    /** Jungle biome identifier */
    public static final CubeBiomeGenBase jungle = (new BiomeGenJungle(21, false)).setColor(5470985).setBiomeName("Jungle").func_76733_a(5470985).setTemperatureAndRainfall(0.95F, 0.9F);
    public static final CubeBiomeGenBase jungleHills = (new BiomeGenJungle(22, false)).setColor(2900485).setBiomeName("JungleHills").func_76733_a(5470985).setTemperatureAndRainfall(0.95F, 0.9F).setHeightRange(field_150591_g);
    public static final CubeBiomeGenBase jungleEdge = (new BiomeGenJungle(23, true)).setColor(6458135).setBiomeName("JungleEdge").func_76733_a(5470985).setTemperatureAndRainfall(0.95F, 0.8F);
    public static final CubeBiomeGenBase deepOcean = (new BiomeGenOcean(24)).setColor(48).setBiomeName("Deep Ocean").setHeightRange(deepOceanRange);
    public static final CubeBiomeGenBase stoneBeach = (new BiomeGenStoneBeach(25)).setColor(10658436).setBiomeName("Stone Beach").setTemperatureAndRainfall(0.2F, 0.3F).setHeightRange(field_150601_k);
    public static final CubeBiomeGenBase coldBeach = (new BiomeGenBeach(26)).setColor(16445632).setBiomeName("Cold Beach").setTemperatureAndRainfall(0.05F, 0.3F).setHeightRange(field_150600_j).setEnableSnow();
    public static final CubeBiomeGenBase birchForest = (new BiomeGenForest(27, 2)).setBiomeName("Birch Forest").setColor(3175492);
    public static final CubeBiomeGenBase birchForestHills = (new BiomeGenForest(28, 2)).setBiomeName("Birch Forest Hills").setColor(2055986).setHeightRange(field_150591_g);
    public static final CubeBiomeGenBase roofedForest = (new BiomeGenForest(29, 3)).setColor(4215066).setBiomeName("Roofed Forest");
    public static final CubeBiomeGenBase coldTaiga = (new BiomeGenTaiga(30, 0)).setColor(3233098).setBiomeName("Cold Taiga").func_76733_a(5159473).setEnableSnow().setTemperatureAndRainfall(-0.5F, 0.4F).setHeightRange(field_150590_f).func_150563_c(16777215);
    public static final CubeBiomeGenBase coldTaigaHills = (new BiomeGenTaiga(31, 0)).setColor(2375478).setBiomeName("Cold Taiga Hills").func_76733_a(5159473).setEnableSnow().setTemperatureAndRainfall(-0.5F, 0.4F).setHeightRange(field_150591_g).func_150563_c(16777215);
    public static final CubeBiomeGenBase megaTaiga = (new BiomeGenTaiga(32, 1)).setColor(5858897).setBiomeName("Mega Taiga").func_76733_a(5159473).setTemperatureAndRainfall(0.3F, 0.8F).setHeightRange(field_150590_f);
    public static final CubeBiomeGenBase megaTaigaHills = (new BiomeGenTaiga(33, 1)).setColor(4542270).setBiomeName("Mega Taiga Hills").func_76733_a(5159473).setTemperatureAndRainfall(0.3F, 0.8F).setHeightRange(field_150591_g);
    public static final CubeBiomeGenBase extremeHillsPlus = (new BiomeGenHills(34, true)).setColor(5271632).setBiomeName("Extreme Hills+").setHeightRange(field_150603_i).setTemperatureAndRainfall(0.2F, 0.3F);
    public static final CubeBiomeGenBase savanna = (new BiomeGenSavanna(35)).setColor(12431967).setBiomeName("Savanna").setTemperatureAndRainfall(1.2F, 0.0F).setDisableRain().setHeightRange(field_150593_e);
    public static final CubeBiomeGenBase SavannaPlateau = (new BiomeGenSavanna(36)).setColor(10984804).setBiomeName("Savanna Plateau").setTemperatureAndRainfall(1.0F, 0.0F).setDisableRain().setHeightRange(field_150602_h);
    public static final CubeBiomeGenBase mesa = (new BiomeGenMesa(37, false, false)).setColor(14238997).setBiomeName("Mesa");
    public static final CubeBiomeGenBase mesaPlateauF = (new BiomeGenMesa(38, false, true)).setColor(11573093).setBiomeName("Mesa Plateau F").setHeightRange(field_150602_h);
    public static final CubeBiomeGenBase mesaPlateau = (new BiomeGenMesa(39, false, false)).setColor(13274213).setBiomeName("Mesa Plateau").setHeightRange(field_150602_h);
    
    protected static final NoiseGeneratorPerlin field_150605_ac;
    protected static final NoiseGeneratorPerlin field_150606_ad;
    protected static final WorldGenDoublePlant field_150610_ae;
    
    public int color;
    public int field_150609_ah;

    /** The block expected to be on the top of this biome */
    public Block topBlock;
    public int field_150604_aj;

    /** The block to fill spots in when not on the top */
    public Block fillerBlock;
    
    public int field_76754_C;

    /** The minimum height of this biome. Default 0.1. */
    public float minHeight;

    /** The maximum height of this biome. Default 0.3. */
    public float maxHeight;

    /** The temperature of this biome. */
    public float temperature;

    /** The rainfall in this biome. */
    public float rainfall;

    /** Color tint applied to water depending on biome */
    public int waterColorMultiplier;

    /** The biome decorator. */
    public BiomeDecorator theBiomeDecorator;

    /**
     * Holds the classes of IMobs (hostile mobs) that can be spawned in the biome.
     */
    protected List spawnableMonsterList;

    /**
     * Holds the classes of any creature that can be spawned in the biome as friendly creature.
     */
    protected List spawnableCreatureList;

    /**
     * Holds the classes of any aquatic creature that can be spawned in the water of the biome.
     */
    protected List spawnableWaterCreatureList;
    protected List spawnableCaveCreatureList;

    /** Set to true if snow is enabled for this biome. */
    protected boolean enableSnow;

    /**
     * Is true (default) if the biome support rain (desert and nether can't have rain)
     */
    protected boolean enableRain;

    /** The id number to this biome, and its index in the biomeList array. */
    public final int biomeID;

    /** The tree generator. */
    protected WorldGenTrees worldGeneratorTrees;

    /** The big tree generator. */
    protected WorldGenBigTree worldGeneratorBigTree;

    /** The swamp tree generator. */
    protected WorldGenSwamp worldGeneratorSwamp;

    protected CubeBiomeGenBase(int biomeID)
    {
    	super(biomeID);
        this.topBlock = Blocks.grass;
        this.field_150604_aj = 0;
        this.fillerBlock = Blocks.dirt;
        this.field_76754_C = 5169201;
        this.minHeight = field_150596_a.field_150777_a;
        this.maxHeight = field_150596_a.field_150776_b;
        this.temperature = 0.5F;
        this.rainfall = 0.5F;
        this.waterColorMultiplier = 16777215;
        
        this.spawnableMonsterList = new ArrayList();
        this.spawnableCreatureList = new ArrayList();
        this.spawnableWaterCreatureList = new ArrayList();
        this.spawnableCaveCreatureList = new ArrayList();
        
        this.enableRain = true;
        
        this.worldGeneratorTrees = new WorldGenTrees(false);
        this.worldGeneratorBigTree = new WorldGenBigTree(false);
        this.worldGeneratorSwamp = new WorldGenSwamp();
        
        this.biomeID = biomeID;
        biomeList[biomeID] = this;
        this.theBiomeDecorator = this.createBiomeDecorator();
        
        this.spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(EntitySheep.class, 12, 4, 4));
        this.spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(EntityPig.class, 10, 4, 4));
        this.spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(EntityChicken.class, 10, 4, 4));
        this.spawnableCreatureList.add(new BiomeGenBase.SpawnListEntry(EntityCow.class, 8, 4, 4));
        
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntitySpider.class, 100, 4, 4));
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntityZombie.class, 100, 4, 4));
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntitySkeleton.class, 100, 4, 4));
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntityCreeper.class, 100, 4, 4));
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntitySlime.class, 100, 4, 4));
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntityEnderman.class, 10, 1, 4));
        this.spawnableMonsterList.add(new BiomeGenBase.SpawnListEntry(EntityWitch.class, 5, 1, 1));
        
        this.spawnableWaterCreatureList.add(new BiomeGenBase.SpawnListEntry(EntitySquid.class, 10, 4, 4));
        
        this.spawnableCaveCreatureList.add(new BiomeGenBase.SpawnListEntry(EntityBat.class, 10, 8, 8));
    }

    /**
     * Allocate a new BiomeDecorator for this BiomeGenBase
     */
    @Override
    protected BiomeDecorator createBiomeDecorator()
    {
        return new BiomeDecorator();
    }

    /**
     * Sets the temperature and rainfall of this biome.
     */
    protected CubeBiomeGenBase setTemperatureAndRainfall(float temp, float rainfall)
    {
        if (temp > 0.1F && temp < 0.2F)
        {
            throw new IllegalArgumentException("Please avoid temperatures in the range 0.1 - 0.2 because of snow");
        }
        else
        {
            this.temperature = temp;
            this.rainfall = rainfall;
            return this;
        }
    }

    protected final CubeBiomeGenBase setHeightRange(CubeBiomeGenBase.Height p_150570_1_)
    {
        this.minHeight = p_150570_1_.field_150777_a;
        this.maxHeight = p_150570_1_.field_150776_b;
        return this;
    }

    /**
     * Disable the rain for the biome.
     */
    protected CubeBiomeGenBase setDisableRain()
    {
        this.enableRain = false;
        return this;
    }

    public WorldGenAbstractTree checkSpawnTree(Random rand)
    {
        return (WorldGenAbstractTree)(rand.nextInt(10) == 0 ? this.worldGeneratorBigTree : this.worldGeneratorTrees);
    }

    /**
     * Gets a WorldGen appropriate for this biome.
     */
    public WorldGenerator getRandomWorldGenForGrass(Random par1Random)
    {
        return new WorldGenTallGrass(Blocks.tallgrass, 1);
    }

    public String spawnFlower(Random rand, int p_150572_2_, int p_150572_3_, int p_150572_4_)
    {
        return rand.nextInt(3) > 0 ? BlockFlower.field_149858_b[0] : BlockFlower.field_149859_a[0];
    }

    /**
     * sets enableSnow to true during biome initialization. returns BiomeGenBase.
     */
    protected CubeBiomeGenBase setEnableSnow()
    {
        this.enableSnow = true;
        return this;
    }

    protected CubeBiomeGenBase setBiomeName(String name)
    {
        this.biomeName = name;
        return this;
    }

    protected CubeBiomeGenBase func_76733_a(int par1)
    {
        this.field_76754_C = par1;
        return this;
    }

    protected CubeBiomeGenBase setColor(int par1)
    {
        this.func_150557_a(par1, false);
        return this;
    }

    protected CubeBiomeGenBase func_150563_c(int p_150563_1_)
    {
        this.field_150609_ah = p_150563_1_;
        return this;
    }

    protected CubeBiomeGenBase func_150557_a(int p_150557_1_, boolean p_150557_2_)
    {
        this.color = p_150557_1_;

        if (p_150557_2_)
        {
            this.field_150609_ah = (p_150557_1_ & 16711422) >> 1;
        }
        else
        {
            this.field_150609_ah = p_150557_1_;
        }

        return this;
    }

    /**
     * takes temperature, returns color
     */
    public int getSkyColorByTemp(float temp)
    {
        temp /= 3.0F;

        if (temp < -1.0F)
        {
            temp = -1.0F;
        }

        if (temp > 1.0F)
        {
            temp = 1.0F;
        }

        return Color.getHSBColor(0.62222224F - temp * 0.05F, 0.5F + temp * 0.1F, 1.0F).getRGB();
    }

    /**
     * Returns the correspondent list of the EnumCreatureType informed.
     */
    public List getSpawnableList(EnumCreatureType par1EnumCreatureType)
    {
        return par1EnumCreatureType == EnumCreatureType.monster ? this.spawnableMonsterList : (par1EnumCreatureType == EnumCreatureType.creature ? this.spawnableCreatureList : (par1EnumCreatureType == EnumCreatureType.waterCreature ? this.spawnableWaterCreatureList : (par1EnumCreatureType == EnumCreatureType.ambient ? this.spawnableCaveCreatureList : null)));
    }

    /**
     * Returns true if the biome have snowfall instead a normal rain.
     */
    public boolean getEnableSnow()
    {
        return this.func_150559_j();
    }

    /**
     * Return true if the biome supports lightning bolt spawn, either by have the bolts enabled and have rain enabled.
     */
    public boolean canSpawnLightningBolt()
    {
        return this.func_150559_j() ? false : this.enableRain;
    }

    /**
     * Checks to see if the rainfall level of the biome is extremely high
     */
    public boolean isHighHumidity()
    {
        return this.rainfall > 0.85F;
    }

    /**
     * returns the chance a creature has to spawn.
     */
    public float getSpawningChance()
    {
        return 0.1F;
    }

//    /**
//     * Gets an integer representation of this biome's rainfall
//     */
//    public final int getIntRainfall()
//    {
//        return (int)(this.rainfall * 65536.0F);
//    }

//    /**
//     * Gets a floating point representation of this biome's rainfall
//     */
//    public final float getFloatRainfall()
//    {
//        return this.rainfall;
//    }

//    /**
//     * Gets a floating point representation of this biome's temperature. above sealevel the temperature decreases, but below it stays constant.
//     */
//    public final float getFloatTemperature(int xAbs, int yAbs, int zAbs)
//    {
//        if (yAbs > 64)
//        {
//            float var4 = (float)field_150605_ac.func_151601_a((double)xAbs * 1.0D / 8.0D, (double)zAbs * 1.0D / 8.0D) * 4.0F;
//            return this.temperature - (var4 + (float)yAbs - 64.0F) * 0.05F / 30.0F;
//        }
//        else
//        {
//            return this.temperature;
//        }
//    }

    public void decorate(World par1World, Random par2Random, int par3, int par4)
    {
        this.theBiomeDecorator.func_150512_a(par1World, par2Random, this, par3, par4);
    }

    /**
     * Provides the basic grass color based on the biome temperature and rainfall
     */
    public int getBiomeGrassColor(int xAbs, int yAbs, int zAbs)
    {
        double var4 = (double)MathHelper.clamp_float(this.getFloatTemperature(xAbs, yAbs, zAbs), 0.0F, 1.0F);
        double var6 = (double)MathHelper.clamp_float(this.getFloatRainfall(), 0.0F, 1.0F);
        return ColorizerGrass.getGrassColor(var4, var6);
    }

    /**
     * Provides the basic foliage color based on the biome temperature and rainfall
     */
    public int getBiomeFoliageColor(int p_150571_1_, int p_150571_2_, int p_150571_3_)
    {
        double var4 = (double)MathHelper.clamp_float(this.getFloatTemperature(p_150571_1_, p_150571_2_, p_150571_3_), 0.0F, 1.0F);
        double var6 = (double)MathHelper.clamp_float(this.getFloatRainfall(), 0.0F, 1.0F);
        return ColorizerFoliage.getFoliageColor(var4, var6);
    }

    public boolean func_150559_j()
    {
        return this.enableSnow;
    }
    
    public void modifyBlocks_pre(World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double val)
    {
        this.modifyBlocks(world, rand, cube, xAbs, yAbs, zAbs, val);
    }
    
    public final void modifyBlocks(World world, Random rand, Cube cube, int xAbs, int yAbs, int zAbs, double val)
    {
        Block topBlock = this.topBlock; // grass/gravel/stone
        byte var11 = (byte)(this.field_150604_aj & 255);
        Block fillBlock = this.fillerBlock; // dirt/gravel/stone
        int var13 = -1;
        int rnd1 = (int)(val / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        
        int xRel = xAbs & 15;
        int yRel = yAbs & 15;
        int zRel = zAbs & 15;
        
        int seaLevel = 63; //replace with user-selectable

        /**Default BuildDepth is 8,388,608. the Earth has a radius of ~6,378,100m. Not too far off.
        * Let's make this world similar to the earth!
        *
        *	Crust - 0 to 35km (varies between 5 and 70km thick due to the sea and mountains)
        *	Upper Mesosphere - 35km to 660km
        *	Lower Mesosphere - 660km to 2890km
        *	Outer Core - 2890km to 5150km
        *	Inner Core - 5150km to 6360km - apparently, the innermost sections of the core could be a plasma! Crazy!
        */
        
        if (yAbs <= BuildSizeEvent.getBuildDepth() + 16 + rand.nextInt(16)) // generate bedrock in the very bottom cube and below plus random bedrock in the cube above that
        {
            cube.setBlockForGeneration(xRel, yRel, zRel, Blocks.bedrock);
        }
        else if (yAbs < -32768 + rand.nextInt(256)) // generate lava sea under y = -32768, plus a rough surface. this is pretty fucking deep though, so nobody will reach this, probably.
        {
        	cube.setBlockForGeneration(xRel, yRel, zRel, Blocks.lava);
        }
        else
        {
            Block block = cube.getBlock(xRel, yRel, zRel);
            
            if (block != null && block.getMaterial() != Material.air)
            {
                if (block == Blocks.stone)
                {
                    if (var13 == -1)
                    {
                        if (rnd1 <= 0)
                        {
                            topBlock = null;
                            var11 = 0;
                            fillBlock = Blocks.stone; // stone/stone/stone
                        }
                        else if (yAbs >= seaLevel - 4 && yAbs <= seaLevel + 1)
                        {
                            topBlock = this.topBlock;
                            var11 = (byte)(this.field_150604_aj & 255);
                            fillBlock = this.fillerBlock;
                        }

                        if (yAbs < seaLevel && (topBlock == null || topBlock.getMaterial() == Material.air))
                        {
                            if (this.getFloatTemperature(xAbs, yAbs, zAbs) < 0.15F)
                            {
                                topBlock = Blocks.ice;
                                var11 = 0;
                            }
                            else
                            {
                                topBlock = Blocks.water;
                                var11 = 0;
                            }
                        }

                        var13 = rnd1;

                        if (yAbs >= 62)
                        {
                        	cube.setBlockForGeneration(xRel, yRel, zRel, topBlock, var11); //grass/gravel/stone
                        }
                        else if (yAbs < 56 - rnd1)
                        {
                            topBlock = null;
                            fillBlock = Blocks.stone; // stone/stone/stone
                            
//                            cubeBlocks.setBlock(xRel, yRel, zRel, Blocks.gravel);
                        }
                        else
                        {
                        	cube.setBlockForGeneration(xRel, yRel, zRel, fillBlock); 
                        }
                    }
                    else if (var13 > 0)
                    {
                        --var13;
                        cube.setBlockForGeneration(xRel, yRel, zRel, fillBlock);

                        if (var13 == 0 && fillBlock == Blocks.sand)
                        {
                            var13 = rand.nextInt(4) + Math.max(0, yAbs - 63);
                            fillBlock = Blocks.sandstone;
                        }
                    }
                }
            }
            else
            {
                var13 = -1;
            }
        }
    }
		
    protected CubeBiomeGenBase func_150566_k()
    {
        return new BiomeGenMutated(this.biomeID + 128, this);
    }

    public Class<? extends CubeBiomeGenBase> func_150562_l()
    {
        return this.getClass();
    }

    public boolean func_150569_a(CubeBiomeGenBase p_150569_1_)
    {
        return p_150569_1_ == this ? true : (p_150569_1_ == null ? false : this.func_150562_l() == p_150569_1_.func_150562_l());
    }

//    @Override
//    public TempCategory func_150561_m()
//    {
//        return (double)this.temperature < 0.2D ? BiomeGenBase.TempCategory.COLD : ((double)this.temperature < 1.0D ? BiomeGenBase.TempCategory.MEDIUM : BiomeGenBase.TempCategory.WARM);
//    }

    public static CubeBiomeGenBase[] getBiomeGenArray()
    {
        return biomeList;
    }

    public static CubeBiomeGenBase getBiome(int val)
    {
        if (val >= 0 && val <= biomeList.length)
        {
            return biomeList[val];
        }
        else
        {
            logger.warn("Biome ID is out of bounds: " + val + ", defaulting to 0 (Ocean)");
            return ocean;
        }
    }

    static
    {
        plains.func_150566_k();
        desert.func_150566_k();
        forest.func_150566_k();
        taiga.func_150566_k();
        swampland.func_150566_k();
        icePlains.func_150566_k();
        jungle.func_150566_k();
        jungleEdge.func_150566_k();
        coldTaiga.func_150566_k();
        savanna.func_150566_k();
        SavannaPlateau.func_150566_k();
        mesa.func_150566_k();
        mesaPlateauF.func_150566_k();
        mesaPlateau.func_150566_k();
        birchForest.func_150566_k();
        birchForestHills.func_150566_k();
        roofedForest.func_150566_k();
        megaTaiga.func_150566_k();
        extremeHills.func_150566_k();
        extremeHillsPlus.func_150566_k();
        biomeList[megaTaigaHills.biomeID + 128] = biomeList[megaTaiga.biomeID + 128];
        CubeBiomeGenBase[] var0 = biomeList;
        int var1 = var0.length;

        for (int var2 = 0; var2 < var1; ++var2)
        {
            CubeBiomeGenBase var3 = var0[var2];

            if (var3 != null && var3.biomeID < 128)
            {
                field_150597_n.add(var3);
            }
        }

        field_150597_n.remove(hell);
        field_150597_n.remove(sky);
        field_150605_ac = new NoiseGeneratorPerlin(new Random(1234L), 1);
        field_150606_ad = new NoiseGeneratorPerlin(new Random(2345L), 1);
        field_150610_ae = new WorldGenDoublePlant();
    }

    public static class SpawnListEntry extends WeightedRandom.Item
    {
        public Class entityClass;
        public int minGroupCount;
        public int maxGroupCount;
        public SpawnListEntry(Class par1Class, int par2, int par3, int par4)
        {
            super(par2);
            this.entityClass = par1Class;
            this.minGroupCount = par3;
            this.maxGroupCount = par4;
        }

        public String toString()
        {
            return this.entityClass.getSimpleName() + "*(" + this.minGroupCount + "-" + this.maxGroupCount + "):" + this.itemWeight;
        }
    }

    public static class Height
    {
        public float field_150777_a;
        public float field_150776_b;
        public Height(float p_i45371_1_, float p_i45371_2_)
        {
            this.field_150777_a = p_i45371_1_;
            this.field_150776_b = p_i45371_2_;
        }

        public CubeBiomeGenBase.Height func_150775_a()
        {
            return new CubeBiomeGenBase.Height(this.field_150777_a * 0.8F, this.field_150776_b * 0.6F);
        }
    }

//    public static enum TempCategory extends net.minecraft.world.biome.BioeGenbase.TempCategory
//    {
//        OCEAN("OCEAN", 0),
//        COLD("COLD", 1),
//        MEDIUM("MEDIUM", 2),
//        WARM("WARM", 3);
//
//        private TempCategory(String p_i45372_1_, int p_i45372_2_) {}
//    }
}
