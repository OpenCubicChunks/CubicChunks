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
package cuchaz.cubicChunks.generator.biome;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.IntCache;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;

public class WorldColumnManager extends WorldChunkManager
{
    private GenLayer genBiomes;

    /** A GenLayer containing the indices into BiomeGenBase.biomeList[] */
    private GenLayer biomeIndexLayer;

    /** The BiomeCache object for this world. */
    private BiomeCache biomeCache;

    /** A list of biomes that the player can spawn in. */
    private List<CubeBiomeGenBase> biomesToSpawnIn;

    protected WorldColumnManager()
    {
        this.biomeCache = new BiomeCache(this);
        this.biomesToSpawnIn = new ArrayList<CubeBiomeGenBase>();
        this.biomesToSpawnIn.add(CubeBiomeGenBase.forest);
        this.biomesToSpawnIn.add(CubeBiomeGenBase.plains);
        this.biomesToSpawnIn.add(CubeBiomeGenBase.taiga);
        this.biomesToSpawnIn.add(CubeBiomeGenBase.taigaHills);
        this.biomesToSpawnIn.add(CubeBiomeGenBase.forestHills);
        this.biomesToSpawnIn.add(CubeBiomeGenBase.jungle);
        this.biomesToSpawnIn.add(CubeBiomeGenBase.jungleHills);
    }

    public WorldColumnManager(long par1, WorldType worldType)
    {
        this();
        GenLayer[] genLayer = GenLayer.initializeAllBiomeGenerators(par1, worldType);
        this.genBiomes = genLayer[0];
        this.biomeIndexLayer = genLayer[1];
    }

    public WorldColumnManager(World world)
    {
        this(world.getSeed(), world.getWorldInfo().getTerrainType());
    }

    /**
     * Gets the list of valid biomes for the player to spawn in.
     */
    @Override
    public List<CubeBiomeGenBase> getBiomesToSpawnIn()
    {
        return this.biomesToSpawnIn;
    }

    /**
     * Returns the BiomeGenBase related to the x, z position on the world.
     */
    @Override
    public CubeBiomeGenBase getBiomeGenAt(int xAbs, int zAbs)
    {
        return this.biomeCache.getBiomeGenAt(xAbs, zAbs);
    }

    /**
     * Returns a list of rainfall values for the specified blocks. Args: listToReuse, x, z, width, length.
     */
    @Override
    public float[] getRainfall(float[] downfall, int cubeX, int cubeZ, int width, int length)
    {
        IntCache.resetIntCache();

        if (downfall == null || downfall.length < width * length)
        {
            downfall = new float[width * length];
        }

        int[] var6 = this.biomeIndexLayer.getInts(cubeX, cubeZ, width, length);

        for (int i = 0; i < width * length; ++i)
        {
            try
            {
//                float rainfall = (float)CubeBiomeGenBase.getBiome(var6[i]).getIntRainfall() / 65536.0F;
            	float rainfall = (float) 0.5;

                if (rainfall > 1.0F)
                {
                    rainfall = 1.0F;
                }

                downfall[i] = rainfall;
            }
            catch (Throwable var11)
            {
                CrashReport var9 = CrashReport.makeCrashReport(var11, "Invalid Biome id");
                CrashReportCategory var10 = var9.makeCategory("DownfallBlock");
                var10.addCrashSection("biome id", Integer.valueOf(i));
                var10.addCrashSection("downfalls[] size", Integer.valueOf(downfall.length));
                var10.addCrashSection("x", Integer.valueOf(cubeX));
                var10.addCrashSection("z", Integer.valueOf(cubeZ));
                var10.addCrashSection("w", Integer.valueOf(width));
                var10.addCrashSection("h", Integer.valueOf(length));
                throw new ReportedException(var9);
            }
        }

        return downfall;
    }

    /**
     * Return an adjusted version of a given temperature based on the y height. (not really).
     */
    @Override
    public float getTemperatureAtHeight(float temp, int height)
    {
        return temp;
    }

    /**
     * Returns an array of biomes for the location input.
     */
    @Override
    public BiomeGenBase[] getBiomesForGeneration( BiomeGenBase[] biomes, int cubeX, int cubeZ, int width, int length )
    {
        IntCache.resetIntCache();

        if (biomes == null || biomes.length < width * length)
        {
            biomes = new CubeBiomeGenBase[width * length];
        }

        int[] intArray = this.genBiomes.getInts(cubeX, cubeZ, width, length);

        try
        {
            for (int i = 0; i < width * length; ++i)
            {
//                biomes[i] = CubeBiomeGenBase.getBiome(intArray[i]);
            	biomes[i] = CubeBiomeGenBase.getBiome(1);
            }

            return biomes;
        }
        catch (Throwable var10)
        {
            CrashReport var8 = CrashReport.makeCrashReport(var10, "Invalid Biome id");
            CrashReportCategory var9 = var8.makeCategory("RawBiomeBlock");
            var9.addCrashSection("biomes[] size", Integer.valueOf(biomes.length));
            var9.addCrashSection("x", Integer.valueOf(cubeX));
            var9.addCrashSection("z", Integer.valueOf(cubeZ));
            var9.addCrashSection("w", Integer.valueOf(width));
            var9.addCrashSection("h", Integer.valueOf(length));
            throw new ReportedException(var8);
        }
    }

    /**
     * Returns biomes to use for the blocks and loads the other data like temperature and humidity onto the
     * WorldChunkManager Args: oldBiomeList, x, z, width, depth
     */
    @Override
    public BiomeGenBase[] loadBlockGeneratorData( BiomeGenBase[] biomes, int cubeX, int cubeZ, int width, int length)
    {
        return this.getBiomeGenAt(biomes, cubeX, cubeZ, width, length, true);
    }

    /**
     * Return a list of biomes for the specified blocks. Args: listToReuse, x, y, width, length, cacheFlag (if false,
     * don't check biomeCache to avoid infinite loop in BiomeCacheBlock)
     */
    @Override
    public BiomeGenBase[] getBiomeGenAt( BiomeGenBase[] biomes, int cubeX, int cubeZ, int width, int length, boolean flag)
    {
        IntCache.resetIntCache();

        if (biomes == null || biomes.length < width * length)
        {
            biomes = new CubeBiomeGenBase[width * length];
        }

        if (flag && width == 16 && length == 16 && (cubeX & 15) == 0 && (cubeZ & 15) == 0)
        {
            CubeBiomeGenBase[] cachedBiomes = this.biomeCache.getCachedBiomes(cubeX, cubeZ);
            System.arraycopy(cachedBiomes, 0, biomes, 0, width * length);
            return biomes;
        }
        else
        {
            int[] aInt = this.biomeIndexLayer.getInts(cubeX, cubeZ, width, length);

            for (int i = 0; i < width * length; ++i)
            {
                biomes[i] = CubeBiomeGenBase.getBiome(aInt[i]);
                
                // make sure we got a valid biome
                assert( biomes[i] != null );
            }

            return biomes;
        }
    }

    /**
     * checks given Chunk's Biomes against List of allowed ones.
     * 
     * this doesn't appear to be used.
     */
    @Override
    public boolean areBiomesViable(int x, int par2, int par3, List list)
    {
        IntCache.resetIntCache();
        int x0 = x - par3 >> 2;
        int z0 = par2 - par3 >> 2;
        int var7 = x + par3 >> 2;
        int var8 = par2 + par3 >> 2;
        int width = var7 - x0 + 1;
        int length = var8 - z0 + 1;
        int[] aInt = this.genBiomes.getInts(x0, z0, width, length);

        try
        {
            for (int i = 0; i < width * length; ++i)
            {
                CubeBiomeGenBase biome = CubeBiomeGenBase.getBiome(aInt[i]);

                if (!list.contains(biome))
                {
                    return false;
                }
            }

            return true;
        }
        catch (Throwable var15)
        {
            CrashReport var13 = CrashReport.makeCrashReport(var15, "Invalid Biome id");
            CrashReportCategory var14 = var13.makeCategory("Layer");
            var14.addCrashSection("Layer", this.genBiomes.toString());
            var14.addCrashSection("x", Integer.valueOf(x));
            var14.addCrashSection("z", Integer.valueOf(par2));
            var14.addCrashSection("radius", Integer.valueOf(par3));
            var14.addCrashSection("allowed", list);
            throw new ReportedException(var13);
        }
    }

    @Override
    @SuppressWarnings( "rawtypes" ) // sampleBlockXZFromSpawnableBiome
	public ChunkPosition func_150795_a( int blockX, int blockZ, int blockDistance, List spawnBiomes, Random rand )
	{
    	// looks like we sample one point of noise for every 4 blocks
		final int BlocksPerSample = 4;
		
    	// sample the noise to get biome data
		IntCache.resetIntCache();
		int minNoiseX = ( blockX - blockDistance )/BlocksPerSample;
		int minNoiseZ = ( blockZ - blockDistance )/BlocksPerSample;
		int maxNoiseX = ( blockX + blockDistance )/BlocksPerSample;
		int maxNoiseZ = ( blockZ + blockDistance )/BlocksPerSample;
		int noiseXSize = maxNoiseX - minNoiseX + 1;
		int noiseZSize = maxNoiseZ - minNoiseZ + 1;
		int[] biomeNoise = genBiomes.getInts( minNoiseX, minNoiseZ, noiseXSize, noiseZSize );
		
		// collect all xz positions from spawnable biomes
		List<ChunkPosition> possibleSpawns = new ArrayList<ChunkPosition>();
		for( int x=0; x<noiseXSize; x++ )
		{
			for( int z=0; z<noiseZSize; z++ )
			{
				CubeBiomeGenBase biome = CubeBiomeGenBase.getBiome( biomeNoise[x + z*noiseXSize] );
				if( spawnBiomes.contains( biome ) )
				{
					int spawnBlockX = ( minNoiseX + x )*BlocksPerSample;
					int spawnBlockZ = ( minNoiseZ + z )*BlocksPerSample;
					possibleSpawns.add( new ChunkPosition( spawnBlockX, 0, spawnBlockZ ) );
				}
			}
		}
		
		if( possibleSpawns.isEmpty() )
		{
			return null;
		}
		
		// pick a random spawn from the list
		return possibleSpawns.get( rand.nextInt( possibleSpawns.size() ) );
	}
    
    /**
     * Calls the WorldChunkManager's biomeCache.cleanupCache()
     */
    @Override
    public void cleanupCache()
    {
        this.biomeCache.cleanupCache();
    }
}
