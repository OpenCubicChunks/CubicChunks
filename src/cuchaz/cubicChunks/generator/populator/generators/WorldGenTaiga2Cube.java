package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenTaiga2Cube extends WorldGenAbstractTreeCube
{
	public WorldGenTaiga2Cube( boolean doBlockNotify )
	{
		super( doBlockNotify );
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int treeHeight = rand.nextInt( 4 ) + 6;
		int heightNoLeaves = 1 + rand.nextInt( 2 );
		int heightLeaves = treeHeight - heightNoLeaves;
		int radiusBase = 2 + rand.nextInt( 2 );
		
		boolean canGenerate = true;

		for( int yAbs = y; yAbs <= y + 1 + treeHeight && canGenerate; ++yAbs )
		{
			int radius = yAbs - y < heightNoLeaves ? 0 : radiusBase;

			for( int xAbs = x - radius; xAbs <= x + radius && canGenerate; ++xAbs )
			{
				for( int zAbs = z - radius; zAbs <= z + radius && canGenerate; ++zAbs )
				{
					Block block = world.getBlock( xAbs, yAbs, zAbs );

					if( block.getMaterial() != Material.air && block.getMaterial() != Material.leaves )
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

		if( blockBelow != Blocks.grass && blockBelow != Blocks.dirt && blockBelow != Blocks.farmland )
		{
			return false;
		}
		
		this.setBlock( world, x, y - 1, z, Blocks.dirt );
		
		int radius = rand.nextInt( 2 );
		int maxRadius = 1;
		byte nextRadius = 0;
		
		//generate leaves
		//go from tree top to bottim increasing leaves radius.
		for( int yFromTreeTop = 0; yFromTreeTop <= heightLeaves; ++yFromTreeTop )
		{
			int yAbs = y + treeHeight - yFromTreeTop;

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
						this.setBlock( world, xAbs, yAbs, zAbs, Blocks.leaves, 1 );
					}
				}
			}

			if( radius >= maxRadius )
			{
				radius = nextRadius;
				nextRadius = 1;
				++maxRadius;

				if( maxRadius > radiusBase )
				{
					maxRadius = radiusBase;
				}
			}
			else
			{
				++radius;
			}
		}

		int leavesHeightAboveLogBlocks = rand.nextInt( 3 );

		for( int yRel = 0; yRel < treeHeight - leavesHeightAboveLogBlocks; ++yRel )
		{
			Block block = world.getBlock( x, y + yRel, z );

			if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
			{
				this.func_150516_a( world, x, y + yRel, z, Blocks.log, 1 );
			}
		}

		return true;
	}
}
