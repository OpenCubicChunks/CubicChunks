package cuchaz.cubicChunks.generator.populator.generators;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenShrubCube extends WorldGenTreesCube
{
	private static final int HEIGHT = 2;
	private final int leavesMetadata;
	private final int logMetadata;

	public WorldGenShrubCube( int par1, int par2 )
	{
		super( false );
		this.logMetadata = par1;
		this.leavesMetadata = par2;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		Block block;

		int minY = getMinBlockYFromRandY( y );
		while( ((block = world.getBlock( x, y, z )).getMaterial() == Material.air || block.getMaterial() == Material.leaves) )
		{
			if( --y < minY )
			{
				return false;
			}
		}

		block = world.getBlock( x, y, z );

		if( block == Blocks.dirt || block == Blocks.grass )
		{
			++y;
			this.setBlock( world, x, y, z, Blocks.log, this.logMetadata );

			for( int yAbs = y; yAbs <= y + HEIGHT; ++yAbs )
			{
				int yDist = yAbs - y;
				int radius = HEIGHT - yDist;

				for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
				{
					int xDist = xAbs - x;

					for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
					{
						int zDist = zAbs - z;

						if( (Math.abs( xDist ) != radius
							|| Math.abs( zDist ) != radius
							|| rand.nextInt( 2 ) != 0)
							&& !world.getBlock( xAbs, yAbs, zAbs ).func_149730_j() )
						{
							this.setBlock( world, xAbs, yAbs, zAbs, Blocks.leaves, this.leavesMetadata );
						}
					}
				}
			}
		}

		return true;
	}
}
