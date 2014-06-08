package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenForestCube extends WorldGenAbstractTreeCube
{
	private final boolean addRandomHeight;

	public WorldGenForestCube( boolean doUpdateNotify, boolean addRandomHeight )
	{
		super( doUpdateNotify );
		this.addRandomHeight = addRandomHeight;
	}

	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int height = rand.nextInt( 3 ) + 5;

		if( this.addRandomHeight )
		{
			height += rand.nextInt( 7 );
		}

		boolean canGenerate = true;

		//can we generate tree here?
		for( int blockY = y; blockY <= y + 1 + height; ++blockY )
		{
			byte radius = 1;

			if( blockY == y )
			{
				radius = 0;
			}

			if( blockY >= y + 1 + height - 2 )
			{
				radius = 2;
			}

			for( int blockX = x - radius; blockX <= x + radius && canGenerate; ++blockX )
			{
				for( int blockZ = z - radius; blockZ <= z + radius && canGenerate; ++blockZ )
				{
					Block block = world.getBlock( blockX, blockY, blockZ );

					if( !this.isReplacableFromTreeGen( block ) )
					{
						canGenerate = false;
					}
				}
			}
		}

		if( !canGenerate )
		{
			return false;
		}

		//Check if we can spawn tree on this block
		Block block = world.getBlock( x, y - 1, z );

		if( block != Blocks.grass && block != Blocks.dirt && block != Blocks.farmland )
		{
			return false;
		}

		//set block below to dirt
		this.setBlock( world, x, y - 1, z, Blocks.dirt );

		//generate leaves
		for( int blockY = y - 3 + height; blockY <= y + height; ++blockY )
		{
			int yRel = blockY - (y + height);
			int radius = 1 - yRel / 2;

			for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
			{
				int xRel = xAbs - x;

				for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
				{
					int zRel = zAbs - z;

					//Don't make square trees
					if( Math.abs( xRel ) != radius || Math.abs( zRel ) != radius || rand.nextInt( 2 ) != 0 && yRel != 0 )
					{
						Block currentBlock = world.getBlock( xAbs, blockY, zAbs );

						if( currentBlock.getMaterial() == Material.air || currentBlock.getMaterial() == Material.leaves )
						{
							this.setBlock( world, xAbs, blockY, zAbs, Blocks.leaves, 2 );
						}
					}
				}
			}
		}

		//generate wood
		for( int yRel = 0; yRel < height; ++yRel )
		{
			Block currentBlock = world.getBlock( x, y + yRel, z );

			if( currentBlock.getMaterial() == Material.air || currentBlock.getMaterial() == Material.leaves )
			{
				this.setBlock( world, x, y + yRel, z, Blocks.log, 2 );
			}
		}

		return true;
	}

	protected void setBlockIfAir( World world, int x, int y, int z )
	{
		Block block = world.getBlock( x, y, z );

		if( block.getMaterial() == Material.air )
		{
			this.setBlock( world, x, y, z, Blocks.leaves2, 1 );
		}
	}
}
