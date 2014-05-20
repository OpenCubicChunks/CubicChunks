package cuchaz.cubicChunks.generator.biome.biomegen;

import java.util.Random;

import net.minecraft.block.BlockFlower;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraft.world.gen.feature.WorldGenBigMushroom;
import net.minecraft.world.gen.feature.WorldGenCactus;
import net.minecraft.world.gen.feature.WorldGenClay;
import net.minecraft.world.gen.feature.WorldGenDeadBush;
import net.minecraft.world.gen.feature.WorldGenFlowers;
import net.minecraft.world.gen.feature.WorldGenLiquids;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraft.world.gen.feature.WorldGenPumpkin;
import net.minecraft.world.gen.feature.WorldGenReed;
import net.minecraft.world.gen.feature.WorldGenSand;
import net.minecraft.world.gen.feature.WorldGenWaterlily;
import net.minecraft.world.gen.feature.WorldGenerator;

public class BiomeDecorator extends net.minecraft.world.biome.BiomeDecorator
{
    /** The world the BiomeDecorator is currently decorating */
    protected World currentWorld;

    /** The Biome Decorator's random number generator. */
    protected Random randomGenerator;

    /** The X-coordinate of the cube currently being decorated */
    protected int cubeX;

    /** The Z-coordinate of the cube currently being decorated */
    protected int cubeZ;

    /** The clay generator. */
    protected WorldGenerator clayGen = new WorldGenClay(4);

    /** The sand generator. */
    protected WorldGenerator sandGen;

    /** The gravel generator. */
    protected WorldGenerator gravelAsSandGen;

    /** The dirt generator. */
    protected WorldGenerator dirtGen;
    protected WorldGenerator gravelGen;
    protected WorldGenerator coalGen;
    protected WorldGenerator ironGen;

    /** Field that holds gold WorldGenMinable */
    protected WorldGenerator goldGen;

    /** Field that holds redstone WorldGenMinable */
    protected WorldGenerator redstoneGen;

    /** Field that holds diamond WorldGenMinable */
    protected WorldGenerator diamondGen;

    /** Field that holds Lapis WorldGenMinable */
    protected WorldGenerator lapisGen;
    
    protected WorldGenFlowers field_150514_p;

    /** Field that holds mushroomBrown WorldGenFlowers */
    protected WorldGenerator mushroomBrownGen;

    /** Field that holds mushroomRed WorldGenFlowers */
    protected WorldGenerator mushroomRedGen;

    /** Field that holds big mushroom generator */
    protected WorldGenerator bigMushroomGen;

    /** Field that holds WorldGenReed */
    protected WorldGenerator reedGen;

    /** Field that holds WorldGenCactus */
    protected WorldGenerator cactusGen;

    /** The water lily generation! */
    protected WorldGenerator waterlilyGen;

    /** Amount of waterlilys per chunk. */
    protected int waterlilyPerChunk;

    /**
     * The number of trees to attempt to generate per chunk. Up to 10 in forests, none in deserts.
     */
    protected int treesPerChunk;

    /**
     * The number of yellow flower patches to generate per chunk. The game generates much less than this number, since
     * it attempts to generate them at a random altitude.
     */
    protected int flowersPerChunk;

    /** The amount of tall grass to generate per chunk. */
    protected int grassPerChunk;

    /**
     * The number of dead bushes to generate per chunk. Used in deserts and swamps.
     */
    protected int deadBushPerChunk;

    /**
     * The number of extra mushroom patches per chunk. It generates 1/4 this number in brown mushroom patches, and 1/8
     * this number in red mushroom patches. These mushrooms go beyond the default base number of mushrooms.
     */
    protected int mushroomsPerChunk;

    /**
     * The number of reeds to generate per chunk. Reeds won't generate if the randomly selected placement is unsuitable.
     */
    protected int reedsPerChunk;

    /**
     * The number of cactus plants to generate per chunk. Cacti only work on sand.
     */
    protected int cactiPerChunk;

    /**
     * The number of sand patches to generate per chunk. Sand patches only generate when part of it is underwater.
     */
    protected int sandPerChunk;

    /**
     * The number of sand patches to generate per chunk. Sand patches only generate when part of it is underwater. There
     * appear to be two separate fields for this.
     */
    protected int sandPerChunk2;

    /**
     * The number of clay patches to generate per chunk. Only generates when part of it is underwater.
     */
    protected int clayPerChunk;

    /** Amount of big mushrooms per chunk */
    protected int bigMushroomsPerChunk;

    /** True if decorator should generate surface lava & water */
    public boolean generateLakes;

    public BiomeDecorator()
    {
        this.sandGen = new WorldGenSand(Blocks.sand, 7);
        this.gravelAsSandGen = new WorldGenSand(Blocks.gravel, 6);
        this.dirtGen = new WorldGenMinable(Blocks.dirt, 32);
        this.gravelGen = new WorldGenMinable(Blocks.gravel, 32);
        this.coalGen = new WorldGenMinable(Blocks.coal_ore, 16);
        this.ironGen = new WorldGenMinable(Blocks.iron_ore, 8);
        this.goldGen = new WorldGenMinable(Blocks.gold_ore, 8);
        this.redstoneGen = new WorldGenMinable(Blocks.redstone_ore, 7);
        this.diamondGen = new WorldGenMinable(Blocks.diamond_ore, 7);
        this.lapisGen = new WorldGenMinable(Blocks.lapis_ore, 6);
        this.field_150514_p = new WorldGenFlowers(Blocks.yellow_flower);
        this.mushroomBrownGen = new WorldGenFlowers(Blocks.brown_mushroom);
        this.mushroomRedGen = new WorldGenFlowers(Blocks.red_mushroom);
        this.bigMushroomGen = new WorldGenBigMushroom();
        this.reedGen = new WorldGenReed();
        this.cactusGen = new WorldGenCactus();
        this.waterlilyGen = new WorldGenWaterlily();
        this.flowersPerChunk = 2;
        this.grassPerChunk = 1;
        this.sandPerChunk = 1;
        this.sandPerChunk2 = 3;
        this.clayPerChunk = 1;
        this.generateLakes = true;
    }

    public void func_150512_a(World world, Random rand, CubeBiomeGenBase biome, int cubeX, int cubeZ)
    {
        if (this.currentWorld != null)
        {
            throw new RuntimeException("Already decorating!!");
        }
        else
        {
            this.currentWorld = world;
            this.randomGenerator = rand;
            this.cubeX = cubeX;
            this.cubeZ = cubeZ;
            this.func_150513_a(biome);
            this.currentWorld = null;
            this.randomGenerator = null;
        }
    }

    protected void func_150513_a(CubeBiomeGenBase biome)
    {
        this.generateOres();
        int var2;
        int var3;
        int var4;

        for (var2 = 0; var2 < this.sandPerChunk2; ++var2)
        {
            var3 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var4 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            this.sandGen.generate(this.currentWorld, this.randomGenerator, var3, this.currentWorld.getTopSolidOrLiquidBlock(var3, var4), var4);
        }

        for (var2 = 0; var2 < this.clayPerChunk; ++var2)
        {
            var3 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var4 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            this.clayGen.generate(this.currentWorld, this.randomGenerator, var3, this.currentWorld.getTopSolidOrLiquidBlock(var3, var4), var4);
        }

        for (var2 = 0; var2 < this.sandPerChunk; ++var2)
        {
            var3 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var4 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            this.gravelAsSandGen.generate(this.currentWorld, this.randomGenerator, var3, this.currentWorld.getTopSolidOrLiquidBlock(var3, var4), var4);
        }

        var2 = this.treesPerChunk;

        if (this.randomGenerator.nextInt(10) == 0)
        {
            ++var2;
        }

        int var5;
        int var6;

        for (var3 = 0; var3 < var2; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.currentWorld.getHeightValue(var4, var5);
            WorldGenAbstractTree var7 = biome.checkSpawnTree(this.randomGenerator);
            var7.setScale(1.0D, 1.0D, 1.0D);

            if (var7.generate(this.currentWorld, this.randomGenerator, var4, var6, var5))
            {
                var7.func_150524_b(this.currentWorld, this.randomGenerator, var4, var6, var5);
            }
        }

        for (var3 = 0; var3 < this.bigMushroomsPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            this.bigMushroomGen.generate(this.currentWorld, this.randomGenerator, var4, this.currentWorld.getHeightValue(var4, var5), var5);
        }

        for (var3 = 0; var3 < this.flowersPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) + 32);
            String var9 = biome.spawnFlower(this.randomGenerator, var4, var6, var5);
            BlockFlower var8 = BlockFlower.func_149857_e(var9);

            if (var8.getMaterial() != Material.air)
            {
                this.field_150514_p.func_150550_a(var8, BlockFlower.func_149856_f(var9));
                this.field_150514_p.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
            }
        }

        for (var3 = 0; var3 < this.grassPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2);
            WorldGenerator var10 = biome.getRandomWorldGenForGrass(this.randomGenerator);
            var10.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
        }

        for (var3 = 0; var3 < this.deadBushPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2);
            (new WorldGenDeadBush(Blocks.deadbush)).generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
        }

        for (var3 = 0; var3 < this.waterlilyPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;

            for (var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2); var6 > 0 && this.currentWorld.isAirBlock(var4, var6 - 1, var5); --var6)
            {
                ;
            }

            this.waterlilyGen.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
        }

        for (var3 = 0; var3 < this.mushroomsPerChunk; ++var3)
        {
            if (this.randomGenerator.nextInt(4) == 0)
            {
                var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
                var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
                var6 = this.currentWorld.getHeightValue(var4, var5);
                this.mushroomBrownGen.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
            }

            if (this.randomGenerator.nextInt(8) == 0)
            {
                var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
                var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
                var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2);
                this.mushroomRedGen.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
            }
        }

        if (this.randomGenerator.nextInt(4) == 0)
        {
            var3 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var4 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var5 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var3, var4) * 2);
            this.mushroomBrownGen.generate(this.currentWorld, this.randomGenerator, var3, var5, var4);
        }

        if (this.randomGenerator.nextInt(8) == 0)
        {
            var3 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var4 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var5 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var3, var4) * 2);
            this.mushroomRedGen.generate(this.currentWorld, this.randomGenerator, var3, var5, var4);
        }

        for (var3 = 0; var3 < this.reedsPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2);
            this.reedGen.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
        }

        for (var3 = 0; var3 < 10; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2);
            this.reedGen.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
        }

        if (this.randomGenerator.nextInt(32) == 0)
        {
            var3 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var4 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var5 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var3, var4) * 2);
            (new WorldGenPumpkin()).generate(this.currentWorld, this.randomGenerator, var3, var5, var4);
        }

        for (var3 = 0; var3 < this.cactiPerChunk; ++var3)
        {
            var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
            var5 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
            var6 = this.randomGenerator.nextInt(this.currentWorld.getHeightValue(var4, var5) * 2);
            this.cactusGen.generate(this.currentWorld, this.randomGenerator, var4, var6, var5);
        }

        if (this.generateLakes)
        {
            for (var3 = 0; var3 < 50; ++var3)
            {
                var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
                var5 = this.randomGenerator.nextInt(this.randomGenerator.nextInt(248) + 8);
                var6 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
                (new WorldGenLiquids(Blocks.flowing_water)).generate(this.currentWorld, this.randomGenerator, var4, var5, var6);
            }

            for (var3 = 0; var3 < 20; ++var3)
            {
                var4 = this.cubeX + this.randomGenerator.nextInt(16) + 8;
                var5 = this.randomGenerator.nextInt(this.randomGenerator.nextInt(this.randomGenerator.nextInt(240) + 8) + 8);
                var6 = this.cubeZ + this.randomGenerator.nextInt(16) + 8;
                (new WorldGenLiquids(Blocks.flowing_lava)).generate(this.currentWorld, this.randomGenerator, var4, var5, var6);
            }
        }
    }

    /**
     * Standard ore generation helper. Generates most ores.
     */
    protected void genStandardOre1(int par1, WorldGenerator par2WorldGenerator, int par3, int par4)
    {
        for (int var5 = 0; var5 < par1; ++var5)
        {
            int var6 = this.cubeX + this.randomGenerator.nextInt(16);
            int var7 = this.randomGenerator.nextInt(par4 - par3) + par3;
            int var8 = this.cubeZ + this.randomGenerator.nextInt(16);
            par2WorldGenerator.generate(this.currentWorld, this.randomGenerator, var6, var7, var8);
        }
    }

    /**
     * Standard ore generation helper. Generates Lapis Lazuli.
     */
    protected void genStandardOre2(int par1, WorldGenerator par2WorldGenerator, int par3, int par4)
    {
        for (int var5 = 0; var5 < par1; ++var5)
        {
            int var6 = this.cubeX + this.randomGenerator.nextInt(16);
            int var7 = this.randomGenerator.nextInt(par4) + this.randomGenerator.nextInt(par4) + (par3 - par4);
            int var8 = this.cubeZ + this.randomGenerator.nextInt(16);
            par2WorldGenerator.generate(this.currentWorld, this.randomGenerator, var6, var7, var8);
        }
    }

    /**
     * Generates ores in the current chunk
     */
    protected void generateOres()
    {
        this.genStandardOre1(20, this.dirtGen, 0, 256);
        this.genStandardOre1(10, this.gravelGen, 0, 256);
        this.genStandardOre1(20, this.coalGen, 0, 128);
        this.genStandardOre1(20, this.ironGen, 0, 64);
        this.genStandardOre1(2, this.goldGen, 0, 32);
        this.genStandardOre1(8, this.redstoneGen, 0, 16);
        this.genStandardOre1(1, this.diamondGen, 0, 16);
        this.genStandardOre2(1, this.lapisGen, 16, 16);
    }
}
