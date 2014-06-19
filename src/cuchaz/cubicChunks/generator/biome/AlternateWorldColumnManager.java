package cuchaz.cubicChunks.generator.biome;

import cuchaz.cubicChunks.generator.biome.biomegen.AlternateBiomeGenTable;
import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.desert;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.extremeHills;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.forest;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.ocean;
import static cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase.plains;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.generator.noise.cache.NoiseCache;
import static cuchaz.cubicChunks.generator.terrain.AlternateTerrainProcessor.CUBE_X_SIZE;
import static cuchaz.cubicChunks.generator.terrain.AlternateTerrainProcessor.CUBE_Z_SIZE;
import static cuchaz.cubicChunks.generator.terrain.AlternateTerrainProcessor.xNoiseSize;
import static cuchaz.cubicChunks.generator.terrain.AlternateTerrainProcessor.zNoiseSize;
import cuchaz.cubicChunks.server.CubeWorldServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.layer.IntCache;

public class AlternateWorldColumnManager extends WorldColumnManager
{
	private int genToNormal[] =
	{
		0, 4, 8, 11, 15
	};//this is to avoid additional caches and almost duplicate methids. 1 block diffference shouldn't be noticable, players don't see temperature/rainfall.
	private NoiseCache volatilityCache = new NoiseCache();
	private NoiseCache heightCache = new NoiseCache();
	private NoiseCache tempCache = new NoiseCache();
	private NoiseCache rainfallCache = new NoiseCache();

	private CubeWorldServer world;

	private final BasicBuilder volatilityBuilder, heightBuilder, tempBuilder, rainfallBuilder;

	private BiomeCache biomeCache;

	private List<CubeBiomeGenBase> biomesToSpawnIn;

	private CubeBiomeGenBase[] biomeList;

	public AlternateWorldColumnManager( CubeWorldServer world )
	{
		this.biomeCache = new BiomeCache( this );
		this.biomesToSpawnIn = new ArrayList<CubeBiomeGenBase>( 7 );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forest );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.plains );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taiga );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.taigaHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.forestHills );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungle );
		this.biomesToSpawnIn.add( CubeBiomeGenBase.jungleHills );

		this.world = world;

		double freqH = 0.003 / (4 * Math.PI);
		double freqV = 0.003 / (4 * Math.PI);
		double freqT = 0.001 / (4 * Math.PI);
		double freqR = 0.001 / (4 * Math.PI);

		Random rand = new Random( world.getSeed() );
		rand.setSeed( rand.nextLong() ^ rand.nextLong() );

		heightBuilder = new BasicBuilder();
		heightBuilder.setSeed( rand.nextInt() );
		heightBuilder.setOctaves( 4 );
		heightBuilder.setMaxElev( 2 );
		heightBuilder.setClamp( -1, 1 );
		heightBuilder.setScale( freqH );
		heightBuilder.build();

		volatilityBuilder = new BasicBuilder();
		volatilityBuilder.setSeed( rand.nextInt() );
		volatilityBuilder.setOctaves( 4 );
		volatilityBuilder.setMaxElev( 2 );
		volatilityBuilder.setClamp( -1, 1 );
		volatilityBuilder.setScale( freqV );
		volatilityBuilder.build();

		tempBuilder = new BasicBuilder();
		tempBuilder.setSeed( rand.nextInt() );
		tempBuilder.setOctaves( 4 );
		tempBuilder.setMaxElev( 1 );
		tempBuilder.setSeaLevel( 0.5 );
		tempBuilder.setClamp( 0, 1 );
		tempBuilder.setScale( freqT );
		tempBuilder.build();

		rainfallBuilder = new BasicBuilder();
		rainfallBuilder.setSeed( rand.nextInt() );
		rainfallBuilder.setOctaves( 4 );
		rainfallBuilder.setMaxElev( 1 );
		rainfallBuilder.setSeaLevel( 0.5 );
		rainfallBuilder.setClamp( 0, 1 );
		rainfallBuilder.setScale( freqR );
		rainfallBuilder.build();
	}

	@Override
	public float[] getRainfall( float[] downfall, int blockX, int blockZ, int width, int length )
	{
		double[][] rain = getRainfallArray( blockX >> 4, blockZ >> 4 );
		if( downfall == null || downfall.length != width * length )
		{
			downfall = new float[width * length];
		}
		for( int x = 0; x < width; x++ )
		{
			for( int z = 0; z < length; z++ )
			{
				downfall[z * length + x] = (float)rain[x][z];//should it be z * length + x or something else?
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
		return getFromCacheOrGenerate( tempCache, columnX, columnZ );
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Rainfall array
	 */
	public double[][] getRainfallArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( rainfallCache, columnX, columnZ );
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Volatility array
	 */
	public double[][] getVolArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( volatilityCache, columnX, columnZ );
	}

	/**
	 * @param columnX
	 * @param columnZ
	 * @return Height array, needs to be interpolated (4x4 by default)
	 */
	public double[][] getHeightArray( int columnX, int columnZ )
	{
		return getFromCacheOrGenerate( heightCache, columnX, columnZ );
	}

	@Override
	public BiomeGenBase[] getBiomeGenAt( BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length, boolean fromCache )
	{
		IntCache.resetIntCache();

		if( biomes == null || biomes.length < width * length )
		{
			biomes = new CubeBiomeGenBase[width * length];
		}
		//if(true) {Arrays.fill( biomes, CubeBiomeGenBase.extremeHills ); return biomes ;}

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
		IntCache.resetIntCache();

		if( biomes == null || biomes.length < width * length )
		{
			biomes = new CubeBiomeGenBase[width * length];
		}
		//if(true) {Arrays.fill( biomes, CubeBiomeGenBase.extremeHills ); return biomes ;}
		int xBlock = xGenBlock * 4;
		int zBlock = zGenBlock * 4;

		assert ((xBlock & 0xf) == 0) && ((zBlock & 0xf) == 0);

		BiomeGenBase biomeArray[] = new BiomeGenBase[16 * 16];
		biomeArray = getBiomeGenAt( biomeArray, xBlock, zBlock, 16, 16, true );

		for( int x = 0; x < 4; x++ )
		{
			for( int z = 0; z < 4; z++ )
			{
				biomes[z * length + x] = biomeArray[genToNormal[z] * 16 + genToNormal[x]];
			}
		}
		return biomes;
	}

	@Override
	public boolean areBiomesViable( int x, int par2, int par3, List list )
	{
		return false;//if it doesn't appear to be used...
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
		//done automatically by Java GC
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

	private double[][] getFromCacheOrGenerate( NoiseCache cache, int columnX, int columnZ )
	{
		double[][] array = cache.getFromCache( columnX, columnZ );
		if( array == null )
		{
			generateAllNoiseArrays( columnX, columnZ );
			array = cache.getFromCache( columnX, columnZ );
			assert array != null;
		}
		return array;
	}

	private void generateAllNoiseArrays( int columnX, int columnZ )
	{
		double[][] volArray = new double[16][16];
		double[][] heightArray = new double[16][16];
		double[][] tempArray = new double[16][16];
		double[][] rainfallArray = new double[16][16];

		populateArray( volArray, volatilityBuilder, columnX * 16, columnZ * 16, 16, 16 );
		populateArray( heightArray, heightBuilder, columnX * 16, columnZ * 16, 16, 16 );
		populateArray( tempArray, tempBuilder, columnX * 16, columnZ * 16, 16, 16 );
		populateArray( rainfallArray, rainfallBuilder, columnX * 16, columnZ * 16, 16, 16 );

		volatilityCache.addToCache( columnX, columnZ, volArray );
		heightCache.addToCache( columnX, columnZ, heightArray );
		tempCache.addToCache( columnX, columnZ, tempArray );
		rainfallCache.addToCache( columnX, columnZ, rainfallArray );
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
				double[][] volArray = getVolArray( x, z );
				double[][] heightArray = getHeightArray( x, z );
				double[][] tempArray = getTempArray( x, z );
				double[][] rainfallArray = getRainfallArray( x, z );
				for( int xRel = 0; xRel < 16; xRel++ )
				{
					for( int zRel = 0; zRel < 16; zRel++ )
					{
						int xAbs = x << 4 | xRel;
						int zAbs = z << 4 | zRel;
						if( xAbs < blockX || zAbs < blockZ || xAbs >= blockX + width || zAbs >= blockZ + length )
						{
							continue;
						}
						double vol = volArray[xRel][zRel];
						double height = heightArray[xRel][zRel];
						double temp = tempArray[xRel][zRel];
						double rainfall = rainfallArray[xRel][zRel];

						rainfall *= temp;

						vol = Math.abs( vol );
						vol *= Math.max( 0, height );
						vol *= 1 - Math.pow( 1 - rainfall, 4 );
						if( height < 0 )
						{
							height *= 0.987;
							height = -Math.pow( -height, MathHelper.clamp_double( -height * 4, 2, 4 ) );
						}

						CubeBiomeGenBase biome = getBiomeForValues( vol, height, temp, rainfall );
						biomes[zRel * length + xRel] = biome;
					}
				}
			}
		}
	}

	private CubeBiomeGenBase getBiomeForValues( double vol, double height, double temp, double rainfall )
	{
		double minDistSquared = Double.MAX_VALUE;
		double minDistSquaredInRange = Double.MAX_VALUE;
		CubeBiomeGenBase nearestBiomeInRange = null;
		CubeBiomeGenBase nearestBiome = null;
		for( AlternateBiomeGenTable.AlternateWorldGenData data: AlternateBiomeGenTable.DATA )
		{
			if( data.biome == null ) continue;
			double maxVol = Math.min( (data.maxHeightWithVol - data.minHeightWithVol) * 0.5, data.maxVolatility );

			double heightWithVolatilityMin = height - vol;
			double heightWithVolatilityMax = height + vol;

			double vAvg = (maxVol + data.minVolatility) * 0.5;

			double hAvg = (data.maxHeightWithVol + data.minHeightWithVol) * 0.5;

			double rAvg = (data.maxRainfall + data.minRainfall) * 0.5;

			double tAvg = (data.maxTemp + data.minTemp) * 0.5;

			boolean heightVolatilityOK = false;
			heightVolatilityOK |= vol <= maxVol && vol >= data.minVolatility
				&& heightWithVolatilityMax <= data.maxHeightWithVol && heightWithVolatilityMin >= data.minHeightWithVol;

			boolean tempRainfallOK = false;
			tempRainfallOK |= temp <= data.maxTemp && temp >= data.minTemp && rainfall <= data.maxRainfall && rainfall >= data.minRainfall;

			double volDist = (vAvg - vol) * (maxVol - data.minVolatility);
			double heightDist = (hAvg - height) * (data.maxHeightWithVol - data.minHeightWithVol);
			double rainfallDist = (rAvg - rainfall) * (data.maxRainfall - data.minRainfall);
			double tempDist = (tAvg - temp) * (data.maxTemp - data.minTemp);

			double distSquared = volDist * volDist + heightDist * heightDist + rainfallDist * rainfallDist + tempDist * tempDist;

			if( !heightVolatilityOK || !tempRainfallOK )
			{

				if( distSquared < minDistSquared )
				{
					nearestBiome = data.biome;
					minDistSquared = distSquared;
				}
				continue;
			}

			if( distSquared < minDistSquaredInRange )
			{
				nearestBiomeInRange = data.biome;
				minDistSquaredInRange = distSquared;
			}
		}

		assert nearestBiome != null;

		/*if( nearestBiomeInRange == null )
		 {
		 System.out.printf( "nearestBiomeInRangeNull, nearestBiome biome: %s\n", nearestBiome.biomeName );
		 }*/
		//return nearest biome in range. If it doesn't exist - return nearest biome
		return nearestBiomeInRange == null ? nearestBiome : nearestBiomeInRange;
	}
}
