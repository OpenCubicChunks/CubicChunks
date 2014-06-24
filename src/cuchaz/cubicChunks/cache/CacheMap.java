package cuchaz.cubicChunks.cache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This is very simple cache for 2d noise. It uses WeakHashMap and array. Stores last n added values.
 */
public class CacheMap<K extends Object, V extends Object> implements Map<K, V>
{
	private final int cacheSize;
	private final WeakHashMap<K, V> cache;
	private int index = 0;
	private final Object[] cacheLastValues;

	public CacheMap( int size, int initialCapacity )
	{
		this.cacheSize = size;
		this.cache = new WeakHashMap<K, V>( initialCapacity );
		this.cacheLastValues = new Object[cacheSize];
	}

	@Override
	public int size()
	{
		return cache.size();
	}

	@Override
	public boolean isEmpty()
	{
		return cache.isEmpty();
	}

	@Override
	public boolean containsKey( Object key )
	{
		return cache.containsKey( key );
	}

	@Override
	public boolean containsValue( Object value )
	{
		return cache.containsValue( value );
	}

	@Override
	public V get( Object key )
	{
		return cache.get( key );
	}

	@Override
	public V put( K key, V value )
	{
		this.addValue( key );
		return cache.put( key, value );
	}

	@Override
	public V remove( Object key )
	{
		//don't remove from array
		return cache.remove( key );
	}

	@Override
	/**
	 * @param m All mappings from this map will be put to cache. If there are more elements that cacheSize IllegalArgumentException is thrown.
	 */
	public void putAll( Map<? extends K, ? extends V> m )
	{
		if( m.size() > cacheSize )
		{
			throw new IllegalArgumentException( "Can't put all values from map to cache. Cache too small. Cache size: " + cacheSize + ", values to add: " + m.size() );
		}

		for( K key: m.keySet() )
		{
			this.addValue( key );
		}

		cache.putAll( m );
	}

	@Override
	public void clear()
	{
		cache.clear();
	}

	@Override
	public Set<K> keySet()
	{
		return cache.keySet();
	}

	@Override
	public Collection<V> values()
	{
		return cache.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet()
	{
		return cache.entrySet();
	}

	private void addValue( K key )
	{
		cacheLastValues[index++] = key;
		index %= cacheSize;
	}
}
