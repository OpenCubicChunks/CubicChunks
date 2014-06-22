package cuchaz.cubicChunks.generator.biome;

import cuchaz.cubicChunks.generator.biome.biomegen.CubeBiomeGenBase;
import cuchaz.cubicChunks.generator.builder.BasicBuilder;
import cuchaz.cubicChunks.generator.builder.IBuilder;
import cuchaz.cubicChunks.cache.CacheMap;
import cuchaz.cubicChunks.generator.biome.biomegen.AlternateBiomeGen;
import static cuchaz.cubicChunks.generator.terrain.GlobalGeneratorConfig.maxElev;
import cuchaz.cubicChunks.server.CubeWorldServer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.util.MathHelper;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.layer.IntCache;

public class AlternateWorldColumnManager extends WorldColumnManager
{

	private static final int DIST_DIMENSIONS = 4;
	//Cache for noise fields.
	private final CacheMap<Long, NoiseArrays> noiseCache = new CacheMap<Long, NoiseArrays>( 1024, 1100 );

	//Builders
	private final BasicBuilder volatilityBuilder, heightBuilder, tempBuilder, rainfallBuilder;
	private final BiomeCache biomeCache;
	private final List<CubeBiomeGenBase> biomesToSpawnIn;

	private final World world;

	public AlternateWorldColumnManager( CubeWorldServer world )
	{
		AlternateBiomeGen.createBuilderForWorld( world );
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
		double freqT = 0.001 / (maxElev / 256) / (4 * Math.PI);
		double freqR = 0.001 / (maxElev / 256) / (4 * Math.PI);

		int octaves = (int)(Math.log( maxElev ));

		Random rand = new Random( world.getSeed() );
		rand.setSeed( rand.nextLong() ^ rand.nextLong() );

		heightBuilder = new BasicBuilder();
		heightBuilder.setSeed( rand.nextInt() );
		heightBuilder.setOctaves( octaves );
		heightBuilder.setMaxElev( 2 );
		heightBuilder.setClamp( -1, 1 );
		heightBuilder.setScale( freqH );
		heightBuilder.build();

		volatilityBuilder = new BasicBuilder();
		volatilityBuilder.setSeed( rand.nextInt() );
		volatilityBuilder.setOctaves( octaves );
		volatilityBuilder.setMaxElev( 1 );
		volatilityBuilder.setClamp( -0.5, 0.5 );
		volatilityBuilder.setScale( freqV );
		volatilityBuilder.build();

		tempBuilder = new BasicBuilder();
		tempBuilder.setSeed( rand.nextInt() );
		tempBuilder.setOctaves( octaves );
		tempBuilder.setMaxElev( 1 );
		tempBuilder.setSeaLevel( 0.5 );
		tempBuilder.setClamp( 0, 1 );
		tempBuilder.setScale( freqT );
		tempBuilder.build();

		rainfallBuilder = new BasicBuilder();
		rainfallBuilder.setSeed( rand.nextInt() );
		rainfallBuilder.setOctaves( octaves );
		rainfallBuilder.setMaxElev( 1 );
		rainfallBuilder.setSeaLevel( 0.5 );
		rainfallBuilder.setClamp( 0, 1 );
		rainfallBuilder.setScale( freqR );
		rainfallBuilder.build();

		this.world = world;
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

	@Override
	public BiomeGenBase[] getBiomeGenAt( BiomeGenBase[] biomes, int blockX, int blockZ, int width, int length, boolean fromCache )
	{
		IntCache.resetIntCache();

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
		//For noise arraysdone automatically by Java GC
		biomeCache.cleanupCache();
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

						height = getRealHeight( height );
						vol = getRealVolatility( vol, height, rainfall, temp );
						
						rainfall *= temp;

						CubeBiomeGenBase biome = getBiomeForValues( x, z, vol, height, temp, rainfall );
						biomes[zRel * length + xRel] = biome;
					}
				}
			}
		}
	}

	private CubeBiomeGenBase getBiomeForValues( double x, double z, double vol, double height, double temp, double rainfall )
	{
		double minDistSquared = Double.MAX_VALUE;
		double minDistSquaredInRange = Double.MAX_VALUE;

		CubeBiomeGenBase nearestBiomeInRange = null;
		CubeBiomeGenBase nearestBiome = null;

		int biomeNum = 0;//used only for generating rarity. Biome order shouldn't change.

		IBuilder rarity = AlternateBiomeGen.getRarityGenerator( this.world );

		//iterate over all biomes and find the "nearest" biome
		for( AlternateBiomeGen.AlternateBiomeGenInfo biomeInfo: AlternateBiomeGen.BIOMES )
		{
			//max volatility for this biome. If we include volatility in height checks volatility can naver be higher that avgHeight - minHeight ( = 0.5*(maxHeight-minHeight) )
			double maxVol = biomeInfo.entendedHeightVolatilityChecks ? Math.min( (biomeInfo.maxHeight - biomeInfo.minHeight) * 0.5, biomeInfo.maxVolatility ) : biomeInfo.maxVolatility;

			//min and max height if we include volatility in height checks
			double heightWithVolatilityMin = height - vol;
			double heightWithVolatilityMax = height + vol;

			//average biome volatility, height, rainfall and temperature
			double vAvg = (maxVol + biomeInfo.minVolatility) * 0.5;
			double hAvg = (biomeInfo.maxHeight + biomeInfo.minHeight) * 0.5;
			double rAvg = (biomeInfo.maxRainfall + biomeInfo.minRainfall) * 0.5;
			double tAvg = (biomeInfo.maxTemp + biomeInfo.minTemp) * 0.5;

			//check if volatility and height are in correct range
			boolean heightVolatilityOK = false;
			//are we between min and max height (including volatility)?
			heightVolatilityOK |= heightWithVolatilityMax <= biomeInfo.maxHeight && heightWithVolatilityMin >= biomeInfo.minHeight;
			//Should we exclude volatility from height checks? So check only height ranges
			heightVolatilityOK |= !biomeInfo.entendedHeightVolatilityChecks && height <= biomeInfo.maxHeight && height >= biomeInfo.minHeight;
			//but always check volatility
			heightVolatilityOK &= vol <= maxVol && vol >= biomeInfo.minVolatility;

			//are temperatire and rainfall in correct range?
			boolean tempRainfallOK = temp <= biomeInfo.maxTemp && temp >= biomeInfo.minTemp && rainfall <= biomeInfo.maxRainfall && rainfall >= biomeInfo.minRainfall;

			//calculate "distance" from average values
			//if biome range is small, distance also should be small so that if biome with broad range fully overlaps biome with tiny range the second biome will be generated.
			double volDist = (vAvg - vol) * (maxVol - biomeInfo.minVolatility);
			double heightDist = (hAvg - height) * (biomeInfo.maxHeight - biomeInfo.minHeight);
			double rainfallDist = (rAvg - rainfall) * (biomeInfo.maxRainfall - biomeInfo.minRainfall);
			double tempDist = (tAvg - temp) * (biomeInfo.maxTemp - biomeInfo.minTemp);

			//don't cache this value. It's generated only once / xz / biome
			//noise values are from -1 to 1. 
			//Corner cases (impossible):
			//	n	r	(n = noise, r = rarity, d = distSquared)
			//----------------------------------------------------
			//	-1	-1	--> rarityModifier = -DIST_DIMENSIONS --> distSquared += DIST_DIMENSIONS; (far biome)
			//	0	-1	--> rarityModifier = -DIST_DIMENSIONS/2 --> distSquared += DIST_DIMENSIONS/2; (far biome)
			//	1	-1	--> rarityModifier = 0 --> distSquared stays the same
			//----------------------------------------------------
			//	-1	1	--> rarityModifier = 0 --> distSquared stays the same
			//	0	1	-->	rarityModifier = DIST_DIMENSIONS/2 --> distSquared -= DIST_DIMENSIONS/2; (near biome)
			//	1	1	--> rarityModifier = DIST_DIMENSIONS --> distSquared -= DIST_DIMENSIONS; (near biome)
			double rarityModifier = rarity.getValue( x / biomeInfo.size, biomeNum, z / biomeInfo.size ) + biomeInfo.rarity;//this is value between -2 and 2. Use it as distSquared (I know, distSquared can't be negative...)
			rarityModifier *= DIST_DIMENSIONS / 2.0D;//now it's from -4 to 4, the same range ad distSquared

			assert rarityModifier > -DIST_DIMENSIONS && rarityModifier < DIST_DIMENSIONS;
			//now calculate distSquared
			double distSquared = volDist * volDist + heightDist * heightDist + rainfallDist * rainfallDist + tempDist * tempDist;
			distSquared -= rarityModifier;

			//Are we in correct value range for the biome?
			if( heightVolatilityOK && tempRainfallOK )
			{
				//is it the "nearest" biome so far?
				if( distSquared < minDistSquaredInRange )
				{
					nearestBiomeInRange = biomeInfo.biome;
					minDistSquaredInRange = distSquared;
				}
			}

			//In case if there is no biome in correct height range we choose the nearest one. So check if it's the nearesat biome so far.
			if( distSquared < minDistSquared )
			{
				nearestBiome = biomeInfo.biome;
				minDistSquared = distSquared;
			}
			biomeNum++;
		}

		assert nearestBiome != null;

		if( nearestBiomeInRange == null )
		{
			System.err.printf( "nearestBiomeInRange == null, nearestBiome biome: %s, %f, %f, %f, %f\n", nearestBiome.biomeName, vol, height, temp, rainfall );
		}
		//return nearest biome in range. If it doesn't exist - return nearest biome
		return nearestBiomeInRange == null ? nearestBiome : nearestBiomeInRange;
	}

	public double getRealVolatility( double volatilityRaw, double heightRaw, double rainfallRaw, double temperatureRaw )
	{
		return Math.min( Math.abs( heightRaw ), (Math.abs( volatilityRaw ) * 0.95D + 0.05) * Math.sqrt( Math.abs( heightRaw ) ) ) * (1 - Math.pow( 1 - rainfallRaw * temperatureRaw, 4 ));
	}

	public double getRealHeight( double heightRaw )
	{
		return heightRaw >= 0 ? heightRaw : -Math.pow( -0.987 * heightRaw, MathHelper.clamp_double( -heightRaw * 4, 2, 4 ) );//Maybe use this function?: ((sin(x*pi - pi/2)^3)/2+0.5)^1.3
	}

	private static Long xzToLong( int x, int z )
	{
		return (x & 0xFFFFFFFFl) | ((z & 0xFFFFFFFFl) << 32);
	}

	private static class NoiseArrays
	{
		private final List<double[][]> arrays = new ArrayList<double[][]>( 4 );

		NoiseArrays( double[][] volatility, double[][] height, double[][] temperature, double[][] rainfall )
		{
			//add 4 null elements to set them later.
			while( arrays.size() < 4 ) arrays.add( null );

			arrays.set( Type.VOLATILITY.ordinal(), volatility );
			arrays.set( Type.HEIGHT.ordinal(), height );
			arrays.set( Type.TEMPERATURE.ordinal(), temperature );
			arrays.set( Type.RAINFALL.ordinal(), rainfall );
		}

		double[][] get( Type type )
		{
			return arrays.get( type.ordinal() );
		}

		enum Type
		{
			VOLATILITY,
			HEIGHT,
			TEMPERATURE,
			RAINFALL
		}
	}
}
