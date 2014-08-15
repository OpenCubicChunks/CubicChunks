package cuchaz.cubicChunks.generator.biome.alternateGen;

import cuchaz.cubicChunks.cache.CacheMap;
import cuchaz.cubicChunks.generator.biome.BiomeCache;
import cuchaz.cubicChunks.generator.biome.WorldColumnManager;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.maxElev;
import cuchaz.cubicChunks.server.CubeWorldServer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import net.minecraft.world.ChunkPosition;
import net.minecraft.world.biome.BiomeGenBase;

public class AlternateWorldColumnManager extends WorldColumnManager
{
	//Cache for noise fields.
	private final CacheMap<Long, NoiseArrays> noiseCache = new CacheMap<Long, NoiseArrays>( 1024, 1100 );
	
	//Cache for noise fields accessed only once every 64 columns
	private final CacheMap<Long, NoiseArrays> noiseCache2 = new CacheMap<Long, NoiseArrays>( 128, 132 );

	//Builders for height volatility temperature and rainfall
	private final BasicBuilder volatilityBuilder, heightBuilder, tempBuilder, rainfallBuilder;
	private final BiomeCache biomeCache;
	private final List<CubeBiomeGenBase> biomesToSpawnIn;

	private final List<BiomeFinder> biomeFindersPriorityList;
	
	private Long worldSeed;
	
	private int zoom = 7;

	private static Long xzToLong( int x, int z )
	{
		return (x & 0xFFFFFFFFl) | ((z & 0xFFFFFFFFl) << 32);
	}

	private AlternateBiomeGen biomeGen;
	
	public AlternateWorldColumnManager( CubeWorldServer world )
	{
		this.worldSeed = world.getSeed();
		this.biomeGen = new AlternateBiomeGen( world );
		this.biomeCache = new BiomeCache( this );
		this.biomesToSpawnIn = new ArrayList<CubeBiomeGenBase>( 7 );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forest );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.plains );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taiga );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taigaHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forestHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungle );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungleHills );

		//this.world = world;
		double freqH = 0.003 / (maxElev / 256) / (4 * Math.PI);
		double freqV = 0.003 / (maxElev / 256) / (4 * Math.PI);
		double freqT = 0.005 / (maxElev / 256) / (4 * Math.PI);
		double freqR = 0.005 / (maxElev / 256) / (4 * Math.PI);

		int octaves = (int)(Math.log( maxElev ));

		Random rand = new Random( world.getSeed() );
		rand.setSeed( rand.nextLong() ^ rand.nextLong() );

		heightBuilder = new BasicBuilder();
		heightBuilder.setSeed( rand.nextInt() );
		heightBuilder.setOctaves( octaves  );
		heightBuilder.setMaxElev( 1.3 );
		heightBuilder.setFreq( freqH );
		heightBuilder.build();

		volatilityBuilder = new BasicBuilder();
		volatilityBuilder.setSeed( rand.nextInt() );
		volatilityBuilder.setOctaves( octaves );
		volatilityBuilder.setMaxElev( 1 );
		volatilityBuilder.setClamp( -0.5, 0.5 );
		volatilityBuilder.setFreq( freqV );
		volatilityBuilder.build();

		tempBuilder = new BasicBuilder();
		tempBuilder.setSeed( rand.nextInt() );
		tempBuilder.setOctaves( octaves );
		tempBuilder.setMaxElev( 0.9);
		tempBuilder.setSeaLevel( 0.5 );
		tempBuilder.setClamp( 0, 1 );
		tempBuilder.setFreq( freqT );
		tempBuilder.build();

		rainfallBuilder = new BasicBuilder();
		rainfallBuilder.setSeed( rand.nextInt() );
		rainfallBuilder.setOctaves( octaves  );
		rainfallBuilder.setMaxElev( 0.9 );
		rainfallBuilder.setSeaLevel( 0.5 );
		rainfallBuilder.setClamp( 0, 1 );
		rainfallBuilder.setFreq( freqR );
		rainfallBuilder.build();


		this.biomeFindersPriorityList = new LinkedList<BiomeFinder>();

		int flags = 0;//default biome finder. Check everything
		this.biomeFindersPriorityList.add( new BiomeFinder( world, biomeGen, flags ) );

		flags |= BiomeFinder.IGNORE_TEMP | BiomeFinder.IGNORE_RAINFALL | BiomeFinder.TEMP_INV | BiomeFinder.RAINFALL_INV;//if no biome found - ignore values player can't see and try again
		this.biomeFindersPriorityList.add( new BiomeFinder( world, biomeGen, flags ) );

		flags |= BiomeFinder.FORCE_NO_EXTENDED_HEIGHT_VOL_CHEKCS;//maybe try without extended height and volatility checks...
		this.biomeFindersPriorityList.add( new BiomeFinder( world, biomeGen, flags ) );

		flags |= BiomeFinder.IGNORE_VOL | BiomeFinder.VOLATILITY_INV | BiomeFinder.NO_RARITY;//If averything else fails - ignore volatility to find something. Ignore rarity.
		this.biomeFindersPriorityList.add( new BiomeFinder( world, biomeGen, flags ) );
			//If still nothing found - give up and throw an Exception...
		//ignoring height would be too risky...
	}

	@Override
	public float[] getRainfall( float[] downfall, int blockX, int blockZ, int width, int length )
	{
		assert width <= 17 && length <= 17;
		
		double[][] rain = getRainfallArray( blockX >> 4, blockZ >> 4 );
		if( downfall == null || downfall.length != width * length )
		{
			downfall = new float[width * length];
		}
		for( int x = 0; x < width; x++ )
		{
			for( int z = 0; z < length; z++ )
			{
				downfall[z * length + x] = (float)rain[x][z];
			}
		}
		return downfall;
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Temperature array
	 */
	public double[][] getTempArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( noiseCache, NoiseArrays.Type.TEMPERATURE, columnX, columnZ );
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Rainfall array
	 */
	public double[][] getRainfallArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( noiseCache, NoiseArrays.Type.RAINFALL, columnX, columnZ );
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Volatility array
	 */
	public double[][] getVolArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( noiseCache, NoiseArrays.Type.VOLATILITY, columnX, columnZ );
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Height array, needs to be interpolated (4x4 by default)
	 */
	public double[][] getHeightArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( noiseCache, NoiseArrays.Type.HEIGHT, columnX, columnZ );
	}
	
	public double  getTempFromCache2( int X, int Z )
	{
		return getFromCache2OrGenerate( noiseCache2, NoiseArrays.Type.TEMPERATURE, X, Z )[0][0];
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Rainfall array
	 */
	public double  getRainfallFromCache2( int X, int Z )
	{
		return getFromCache2OrGenerate( noiseCache2, NoiseArrays.Type.RAINFALL, X, Z )[0][0];
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Volatility array
	 */
	public double  getVolFromCache2( int X, int Z )
	{
		return getFromCache2OrGenerate( noiseCache2, NoiseArrays.Type.VOLATILITY, X, Z )[0][0];
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Height array, needs to be interpolated (4x4 by default)
	 */
	public double  getHeightFromCache2( int X, int Z )
	{
		return getFromCache2OrGenerate( noiseCache2, NoiseArrays.Type.HEIGHT, X, Z )[0][0];
	}
	
	public double getHeight( int x, int z )
	{
		return heightBuilder.getValue( x, 0, z );
	}

	public double getVolatility( int x, int z )
	{
		return volatilityBuilder.getValue( x, 0, z );
	}

	public double getTemp( int x, int z )
	{
		return tempBuilder.getValue( x, 0, z );
	}

	public double getRainfall( int x, int z )
	{
		return rainfallBuilder.getValue( x, 0, z );
	}

	@Override
	public BiomeGenBase[] getBiomeGenAt( BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length, boolean fromCache )
	{
		//IntCache.resetIntCache();

		if( biomes == null || biomes.length < width * length )
		{
			biomes = new CubeBiomeGenBase[width * length];
		}

		if( fromCache && width == 16 && length == 16 && (blockX & 15) == 0 && (blockZ & 15) == 0 )
		{
			CubeBiomeGenBase[] cachedBiomes = this.biomeCache.getCachedBiomes( blockX, blockZ );
			System.arraycopy( cachedBiomes, 0, biomes, 0, width * length );
			return biomes;
		}
		else
		{
			this.generateBiomes( biomes, blockX, blockZ, width, length );

			return biomes;
		}
	}

	@Override
	public BiomeGenBase[] getBiomesForGeneration( BiomeGenBase[] biomes, int xGenBlock, int zGenBlock, int width, int length )
	{
		//this is unuses=d
		throw new UnsupportedOperationException( "AlternateWrldColumnManager.getBiomesForGeneration()" );
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean areBiomesViable( int x, int par2, int par3, List list )
	{
		//This is probably unused. No crash so far...
		throw new UnsupportedOperationException( "AlternateWrldColumnManager.areBiomesViable()" );
	}

	@Override
	@SuppressWarnings("rawtypes") // sampleBlockXZFromSpawnableBiome
	public ChunkPosition func_150795_a( int blockX, int blockZ, int blockDistance, List spawnBiomes, Random rand )
	{
		//TODO: getBlockXZFromSpawnableBiome
		return new ChunkPosition( 0, 0, 0 );
		/*// looks like we sample one point of noise for every 4 blocks
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
		 return possibleSpawns.get( rand.nextInt( possibleSpawns.size() ) );*/
	}

	@Override
	public void cleanupCache()
	{
		//For noise arrays done automatically by Java GC
		biomeCache.cleanupCache();
	}

	public double getRealVolatility( double volatilityRaw, double heightRaw, double rainfallRaw, double temperatureRaw )
	{
		return Math.min( Math.abs( heightRaw ), (Math.abs( volatilityRaw ) * 0.95D + 0.05) * Math.sqrt( Math.abs( heightRaw * heightRaw ) ) ) * (1 - Math.pow( 1 - rainfallRaw * temperatureRaw, 4 ));
	}

	public double getRealHeight( double heightRaw )
	{
		//return (heightRaw * 1.4 + heightRaw*heightRaw*Math.sin(heightRaw*17)) * 30;
		//Maybe use this function?: ((sin(x*pi - pi/2)^3)/2+0.5)^1.3
		if (heightRaw < -0.0){
			return heightRaw;//ocean
		}
		else if (heightRaw > 0.0 && heightRaw <= 0.05){
			return heightRaw * 0.2D;//beach, swamp
		}
		else if (heightRaw > 0.05 && heightRaw <= 0.3){
			return heightRaw * 0.5D - 0.015D;//plains, savanna, etc.
		}
		else if (heightRaw > 0.3 && heightRaw <= 0.6){
			return heightRaw - 0.165D;//forest, desert, mesa, etc.
		}
		else if (heightRaw > 0.6 && heightRaw <= 0.8){
			return 1.5D * heightRaw - 0.465D;//hill biomes
		}
		return 1.75 * heightRaw - 0.065D - 0.6;//extreme hills, ice mountains
	}

	private double[][] populateArray( double[][] array, IBuilder builder, int startX, int startZ, int xSize, int zSize )
	{
		if( array == null || array.length != xSize || array[0].length != zSize )
		{
			throw new IllegalArgumentException();
		}
		for( int x = 0; x < xSize; x++ )
		{
			for( int z = 0; z < zSize; z++ )
			{
				array[x][z] = builder.getValue( startX + x, 0, startZ + z );
			}
		}
		return array;
	}

	private double[][] getFromCacheOrGenerate( CacheMap<Long, NoiseArrays> cache, NoiseArrays.Type type, int columnX, int columnZ )
	{
		NoiseArrays arrays = cache.get( xzToLong( columnX, columnZ ) );
		if( arrays == null )
		{
			generateAllNoiseArrays( columnX, columnZ );
			arrays = cache.get( xzToLong( columnX, columnZ ) );
			assert arrays != null;
		}
		return arrays.get( type );
	}
	
	private double[][] getFromCache2OrGenerate( CacheMap<Long, NoiseArrays> cache, NoiseArrays.Type type, int X, int Z )
	{
		X = (X >> zoom) << zoom;
		Z = (Z >> zoom) << zoom;
		NoiseArrays arrays = cache.get( xzToLong( X, Z ) );
		if( arrays == null )
		{
			addToNoiseCache2( X, Z );
			arrays = cache.get( xzToLong( X, Z ) );
			assert arrays != null;
		}
		return arrays.get( type );
	}
	
	private void addToNoiseCache2( int X, int Z )
	{
		double[][] vol = new double [1][1];
		double[][] height = new double [1][1];
		double[][] temp = new double [1][1];
		double [][] rainfall = new double [1][1];
		vol[0][0] = this.getVolatility( X, Z);
		height[0][0] = this.getHeight( X, Z);
		temp[0][0] = this.getTemp( X, Z);
		rainfall[0][0] = this.getRainfall( X, Z);
		
		NoiseArrays arrays2 = new NoiseArrays(vol, height, temp, rainfall);
		noiseCache2.put( xzToLong(  X, Z) , arrays2);
	}

	private void generateAllNoiseArrays( int columnX, int columnZ )
	{
		//yes, length 17. This is correct.
		double[][] volArray = new double[17][17];
		double[][] heightArray = new double[17][17];
		double[][] tempArray = new double[17][17];
		double[][] rainfallArray = new double[17][17];

		//and multimply position by 16, not 17. This is correct too. In AlternateTerrainProcessor we need values at 0, 4, 8, 12 and 16. Value at 16 is 17-th array element.
		populateArray( volArray, volatilityBuilder, columnX * 16, columnZ * 16, 17, 17 );
		populateArray( heightArray, heightBuilder, columnX * 16, columnZ * 16, 17, 17 );
		populateArray( tempArray, tempBuilder, columnX * 16, columnZ * 16, 17, 17 );
		populateArray( rainfallArray, rainfallBuilder, columnX * 16, columnZ * 16, 17, 17 );
		
		NoiseArrays arrays = new NoiseArrays( volArray, heightArray, tempArray, rainfallArray );
		noiseCache.put( xzToLong( columnX, columnZ ), arrays );
	}

	private void generateBiomes( BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length )
	{
		int minChunkX = blockX >> 4;
		int minChunkZ = blockZ >> 4;
		int maxChunkX = (blockX + width - 1) >> 4;
		int maxChunkZ = (blockZ + length - 1) >> 4;

		assert minChunkX <= maxChunkX;
		assert minChunkZ <= maxChunkZ;

		for( int x = minChunkX; x <= maxChunkX; x++ )
		{
			for( int z = minChunkZ; z <= maxChunkZ; z++ )
			{
				double[][] heightArray = this.getHeightArray(x, z);
				double[][] volArray = getVolArray( x, z );
				double[][] tempArray = getTempArray( x, z );
				double[][] rainfallArray = getRainfallArray( x, z );
				for( int xRel = 0; xRel < 16; xRel++ )
				{
					for( int zRel = 0; zRel < 16; zRel++ )
					{
						int [] coords = getAlteredCoordinates((x<<4)+xRel,(z<<4)+zRel,this.worldSeed);
						double height;
						double vol;
						double rainfall;
						double temp;
						CubeBiomeGenBase biome;
						if (heightArray[xRel][zRel] > 0.05)
						{
							int x1 = coords[0];
							int z1 = coords[1];
							int chunkX = x1 >> 4;
							int chunkZ = z1 >> 4;
							vol = getVolFromCache2(x1,z1);
							height = getHeightFromCache2(x1,z1);
							temp = getTempFromCache2(x1,z1);
							rainfall = getRainfallFromCache2(x1,z1);
							height = height > 1 ? 1 : height;
							height = height < -1 ? -1 : height;
							vol = getRealVolatility( vol, height, rainfall, temp );
							height = (height < 0.05) ?  0.05 : height;
							biome = getBiomeForValues( chunkX, chunkZ, vol, height, temp, rainfall );
						}
						else
						{
							vol = volArray[xRel][zRel];
							height = heightArray[xRel][zRel];
							temp = tempArray[xRel][zRel];
							rainfall = rainfallArray[xRel][zRel];
							height = height > 1 ? 1 : height;
							height = height < -1 ? -1 : height;
							vol = getRealVolatility( vol / 2, height, rainfall, temp );

							biome = getBiomeForValues( x, z, vol, height, temp, rainfall );
						}

						//height = getRealHeight( height );

						biomes[zRel * length + xRel] = biome;
					}
				}
			}
		}
	}

	private CubeBiomeGenBase getBiomeForValues( double x, double z, double vol, double height, double temp, double rainfall )
	{
		CubeBiomeGenBase biome = null;
		for( BiomeFinder finder: biomeFindersPriorityList )
		{
			biome = finder.getBiomeForValues( x, z, vol, height, temp, rainfall );
			if( biome != null )
			{
				break;
			}
		}

		if( biome == null )
		{
			throw new RuntimeException( String.format( "No biome for values found: h = %f, v = %f, t = %f, r = %f", height, vol, temp, rainfall ) );
		}

		return biome;
	}
	
	public int [] getAlteredCoordinates(int x, int z, Long seed)
	{
		int zoom = this.zoom;
		long orgseed = seed;
		int [] array1 = new int[] {1, 2, 3, 4};
		int [] array2 = new int[] {0, 0, 0, 0, 0, 0};
		for (int i = zoom; i >= 0; i--)
		{
			seed = initChunkSeed(x >> (i+1),z >> (i+1), orgseed+i);
			array2[0] = array1[0];
			array2[1] = array1[this.nextInt(2, seed)];
			seed *= seed * 6364136223846793005L + 1442695040888963407L;
            seed += this.worldSeed;
			array2[2] = array1[this.nextInt(2,seed) << 1];
			seed *= seed * 6364136223846793005L + 1442695040888963407L;
            seed += this.worldSeed;
			array2[3] = array1[0]==array1[1] && array1[0]==array1[2]?array1[0]:(array1[1]==array1[2] && array1[1] == array1[3]?array1[1]:((array1[0]==array1[1] && array1[2] != array1[3]) || (array1[0]==array1[2] && array1[1] != array1[3]) || (array1[0]==array1[3] && array1[1] != array1[2])?array1[0]:( (array1[1]==array1[2] && array1[0] != array1[3]) || (array1[1]==array1[2] && array1[0] != array1[3])?array1[1]:array1[2]==array1[3] && array1[0] != array1[1]?array1[2]:array1[this.nextInt(4,seed)])));
			seed = initChunkSeed(x >> (i+1),(z >> (i+1))+1, orgseed+i);
			array2[5] = array1[this.nextInt(2, seed) + 2];
			seed = initChunkSeed((x >> (i+1))+1,z >> (i+1), orgseed+i);
			seed *= seed * 6364136223846793005L + 1442695040888963407L;
            seed += this.worldSeed;
			array2[4] = array1[(this.nextInt(2, seed) << 1) + 1];
			int q1 = (x >> i)&1;
			int q2 = (z >> i)&1;
			assert((q1==0 || q1 == 1) && (q2 == 0 || q2 == 1));
			if (q1==0 && q2 == 0)
			{
				array1[1] = array2[1];
				array1[2] = array2[2];
				array1[3] = array2[3];
			}
			else if (q1 == 1 && q2 == 0)
			{
				array1[0] = array2[1];
				array1[2] = array2[3];
				array1[3] = array2[4];
			}
			else if (q1 == 0 && q2 == 1)
			{
				array1[0] = array2[2];
				array1[1] = array2[3];
				array1[3] = array2[5];
			}
			else
			{
				array1[0] = array2[3];
				array1[1] = array2[4];
				array1[2] = array2[5];
			}
			if (array1[0]==array1[1] && array1[0]==array1[2] && array1[0] == array1[3])
			{
				break;
			}
		}
		zoom = zoom+1;
		if (array2[3] == 1)
		{
			x = (x >> zoom) << zoom;
			z = (z >> zoom) << zoom;
		}
		else if (array2[3] == 2)
		{
			x = ((x >> zoom) + 1) << zoom;
			z = (z >> zoom) << zoom;
		}
		else if (array2[3] == 3)
		{
			x = (x >> zoom) << zoom;
			z = ((z >> zoom) + 1) << zoom;
		}
		else
		{
			x = ((x >> zoom) + 1) << zoom;
			z = ((z >> zoom) + 1) << zoom;
		}
		
		return new int []  {x, z};
	}
	
    /**
     * Initialize layer's current chunkSeed based on the local worldGenSeed and the (x,z) chunk coordinates.
     */
    public long initChunkSeed(long x, long z, long seed)
    {
        seed *= seed * 6364136223846793005L + 1442695040888963407L;
        seed += x;
        seed *= seed * 6364136223846793005L + 1442695040888963407L;
        seed += z;
        seed *= seed * 6364136223846793005L + 1442695040888963407L;
        seed += x;
        seed *= seed * 6364136223846793005L + 1442695040888963407L;
        seed += z;
        return seed;
    }

    /**
     * returns a LCG pseudo random number from [0, x). Args: int x
     */
    protected int nextInt(int par1, Long seed)
    {
        int var2 = (int)((seed >> 24) % (long)par1);

        if (var2 < 0)
        {
            var2 += par1;
        }
        return var2;
    }
}