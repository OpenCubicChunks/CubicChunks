package cuchaz.cubicChunks.lighting;

import java.util.List;

import cuchaz.cubicChunks.CubeProvider;
import cuchaz.cubicChunks.util.Bits;
import cuchaz.cubicChunks.util.Coords;
import cuchaz.cubicChunks.world.BlankColumn;
import cuchaz.cubicChunks.world.Column;

public abstract class BlockColumnCalculator implements LightCalculator
{
	@Override
	public int processBatch( List<Long> addresses, List<Long> deferredAddresses, CubeProvider provider )
	{
		// start processing
		int numSuccesses = 0;
		for( long address : addresses )
		{
			// get the block coords
			int blockX = Bits.unpackSigned( address, 26, 0 );
			int blockZ = Bits.unpackSigned( address, 26, 26 );
			
			// get the column
			int cubeX = Coords.blockToCube( blockX );
			int cubeZ = Coords.blockToCube( blockZ );
			Column column = (Column)provider.provideChunk( cubeX, cubeZ );
			
			// skip blank columns
			if( column == null || column instanceof BlankColumn )
			{
				continue;
			}
			
			// get the local coords
			int localX = Coords.blockToLocal( blockX );
			int localZ = Coords.blockToLocal( blockZ );
			
			// add unsuccessful calculations back onto the queue
			boolean success = calculate( column, localX, localZ, blockX, blockZ );
			if( !success )
			{
				deferredAddresses.add( address );
			}
			else
			{
				numSuccesses++;
			}
		}
		return numSuccesses;
	}
	
	public abstract boolean calculate( Column column, int localX, int localZ, int blockX, int blockZ );
}
