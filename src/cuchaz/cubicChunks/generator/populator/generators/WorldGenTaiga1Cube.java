package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenTaiga1Cube extends WorldGenAbstractTreeCube
{
	public WorldGenTaiga1Cube()
	{
		super( false );
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int treeHeight = rand.nextInt( 5 ) + 7;
		int heightLeaves = treeHeight - rand.nextInt( 2 ) - 3;
		int radiusBase = 1 + rand.nextInt( treeHeight - heightLeaves + 1 );
		boolean canGenerate = true;

		for( int yAbs = y; yAbs <= y + 1 + treeHeight && canGenerate; ++yAbs )
		{
			int radius = yAbs - y < heightLeaves ? 0 : radiusBase;

			for( int xAbs = x - radius; xAbs <= x + radius && canGenerate; ++xAbs )
			{
				for( int zAbs = z - radius; zAbs <= z + radius && canGenerate; ++zAbs )
				{
					Block block = world.getBlock( xAbs, yAbs, zAbs );

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

		Block blockBelow = world.getBlock( x, y - 1, z );

		if( blockBelow != Blocks.grass && blockBelow != Blocks.dirt )
		{
			return false;
		}
		
		this.setBlock( world, x, y - 1, z, Blocks.dirt );
		int radius = 0;

		for( int yAbs = y + treeHeight; yAbs >= y + heightLeaves; --yAbs )
		{
			for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
			{
				int xRel = xAbs - x;

				for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
				{
					int zRel = zAbs - z;

					if( (Math.abs( xRel ) != radius || 
						Math.abs( zRel ) != radius || 
						radius <= 0) && 
						!world.getBlock( xAbs, yAbs, zAbs ).func_149730_j() )
					{
						this.setBlock(world, xAbs, yAbs, zAbs, Blocks.leaves, 1 );
					}
				}
			}

			if( radius >= 1 && yAbs == y + heightLeaves + 1 )
			{
				--radius;
			}
			else if( radius < radiusBase )
			{
				++radius;
			}
		}

		for( int yRel = 0; yRel < treeHeight - 1; ++yRel )
		{
			Block block = world.getBlock( x, y + yRel, z );

			if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
			{
				this.setBlock(world, x, y + yRel, z, Blocks.log, 1 );
			}
		}

		return true;
	}
}
