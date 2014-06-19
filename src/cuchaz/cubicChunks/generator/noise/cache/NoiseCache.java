package cuchaz.cubicChunks.generator.noise.cache;

import java.util.Arrays;
import java.util.WeakHashMap;

/**
 * This is very simple cache for 2d noise. It uses WeakHashMap and array.
 */
public class NoiseCache
{
	private static final int CACHE_SIZE = 1024;
	private final WeakHashMap<Long, NoiseArray> cache = new WeakHashMap<Long, NoiseArray>( (int)(CACHE_SIZE * 1.2) );
	private int index = 0;
	private NoiseArray[] cacheLastValues = new NoiseArray[CACHE_SIZE];

	private static long getMapValue( int x, int z )
	{
		return (x & 0xFFFFFFFFl) | ((z & 0xFFFFFFFFl) << 32);
	}

	public void addToCache( int x, int z, double[][] noise )
	{
		//if our cache already contains this value we replace it. Don't replace it in array. The array exists only to let GC delete the values after other CACHE_SIZE values are cached
		long mapIndex = getMapValue( x, z );
		NoiseArray array = new NoiseArray( x, z, noise );
		cacheLastValues[index++] = array;
		index %= CACHE_SIZE;
		cache.put( mapIndex, array );
	}

	public double[][] getFromCache( int x, int z )
	{
		long mapIndex = getMapValue( x, z );
		NoiseArray a = cache.get( mapIndex );
		return a == null ? null : a.getArray();
	}

	private static class NoiseArray
	{
		private int x;
		private int z;
		private double[][] array;

		NoiseArray( int x, int z, double[][] noise )
		{
			array = Arrays.copyOf( noise, noise.length );
			this.x = x;
			this.z = z;
		}

		public int getX()
		{
			return x;
		}

		public int getZ()
		{
			return z;
		}

		public double[][] getArray()
		{
			return array;
		}
	}
}
