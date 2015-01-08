package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenIcePathCube extends WorldGeneratorCube
{
	private final Block block;
	private int s;

	public WorldGenIcePathCube( int s )
	{
		this.block = Blocks.packed_ice;
		this.s = s;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int minY = getMinBlockYFromRandY( y );
		while( world.isAirBlock( x, y, z ) )
		{
			if( --y < minY )
			{
				return false;
			}
		}

		if( world.getBlock( x, y, z ) != Blocks.snow )
		{
			return false;
		}
		
		int radius = rand.nextInt( this.s - 2 ) + 2;
		byte yRadius = 1;

		for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
		{
			for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
			{
				int xDist = xAbs - x;
				int zDist = zAbs - z;

				if( xDist * xDist + zDist * zDist <= radius * radius )
				{
					for( int yAbs = y - yRadius; yAbs <= y + yRadius; ++yAbs )
					{
						Block block = world.getBlock( xAbs, yAbs, zAbs );

						if( block == Blocks.dirt || block == Blocks.snow || block == Blocks.ice )
						{
							world.setBlock( xAbs, yAbs, zAbs, this.block, 0, 2 );
						}
					}
				}
			}
		}

		return true;
	}
}
