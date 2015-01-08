package cuchaz.cubicChunks.generator.populator;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenHugeTrees;

public abstract class WorldGenHugeTreesCube extends WorldGenAbstractTreeCube
{

	/** The base height of the tree */
	protected final int baseHeight;

	/** Sets the metadata for the wood blocks used */
	protected final int woodMetadata;

	/** Sets the metadata for the leaves used in huge trees */
	protected final int leavesMetadata;
	protected int maxRandAdditionalHeight;

	public WorldGenHugeTreesCube( boolean flag, int baseHeight, int maxRandAdditionalHeight, int woodMeta, int leavesMeta )
	{
		super( flag );
		this.baseHeight = baseHeight;
		this.woodMetadata = woodMeta;
		this.leavesMetadata = leavesMeta;
		this.maxRandAdditionalHeight = maxRandAdditionalHeight;
	}

	protected int getRandomHeight( Random rand )
	{
		int val = rand.nextInt( 3 ) + this.baseHeight;

		if( this.maxRandAdditionalHeight > 1 )
		{
			val += rand.nextInt( this.maxRandAdditionalHeight );
		}

		return val;
	}

	protected boolean canGenerate( World world, Random rand, int x, int y, int z, int radius )
	{
		return this.isEnoughSpaceToGenerate( world, rand, x, y, z, radius ) && this.canGenerateOnBlockBelow( world, rand, x, y, z );
	}

	protected void generateLayerAtLocationWithRadius2x2( World world, int x, int y, int z, int radius, Random rand )
	{
		int radiusSquared = radius * radius;

		for( int xAbs = x - radius; xAbs <= x + radius + 1; ++xAbs )
		{
			int xDist = xAbs - x;

			for( int zAbs = z - radius; zAbs <= z + radius + 1; ++zAbs )
			{
				int zDist = zAbs - z;
				//2x2 tree
				int xDistMinusOne = xDist - 1;
				int zDistMinusOne = zDist - 1;

				if( xDist * xDist + zDist * zDist <= radiusSquared
					|| xDistMinusOne * xDistMinusOne + zDistMinusOne * zDistMinusOne <= radiusSquared
					|| xDist * xDist + zDistMinusOne * zDistMinusOne <= radiusSquared
					|| xDistMinusOne * xDistMinusOne + zDist * zDist <= radiusSquared )
				{
					Block block = world.getBlock( xAbs, y, zAbs );

					if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
					{
						this.setBlock( world, xAbs, y, zAbs, Blocks.leaves, this.leavesMetadata );
					}
				}
			}
		}
	}

	protected void generateLayerAtLocationWithRadius( World world, int x, int y, int z, int radius, Random rand )
	{
		int radiusSquared = radius * radius;

		for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
		{
			int xDist = xAbs - x;

			for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
			{
				int zDist = zAbs - z;

				if( xDist * xDist + zDist * zDist <= radiusSquared )
				{
					Block block = world.getBlock( xAbs, y, zAbs );

					if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
					{
						this.setBlock( world, xAbs, y, zAbs, Blocks.leaves, this.leavesMetadata );
					}
				}
			}
		}
	}

	protected void setBlockIfAir( World world, int x, int y, int z )
	{
		Block block = world.getBlock( x, y, z );

		if( block.getMaterial() == Material.air )
		{
			this.setBlock( world, x, y, z, Blocks.leaves2, 1 );
		}
	}

	private boolean isEnoughSpaceToGenerate( World world, Random rand, int x, int y, int z, int height )
	{
		boolean canGenerate = true;

		for( int yAbs = y; yAbs <= y + 1 + height; ++yAbs )
		{
			byte radius = 2;

			if( yAbs == y )
			{
				radius = 1;
			}

			if( yAbs >= y + 1 + height - 2 )
			{
				radius = 2;
			}

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

		return canGenerate;
	}

	private boolean canGenerateOnBlockBelow( World world, Random rand, int x, int y, int z )
	{
		Block block = world.getBlock( x, y - 1, z );

		if( (block == Blocks.grass || block == Blocks.dirt) )
		{
			world.setBlock( x, y - 1, z, Blocks.dirt, 0, 2 );
			world.setBlock( x + 1, y - 1, z, Blocks.dirt, 0, 2 );
			world.setBlock( x, y - 1, z + 1, Blocks.dirt, 0, 2 );
			world.setBlock( x + 1, y - 1, z + 1, Blocks.dirt, 0, 2 );
			return true;
		}
		return false;
	}
}
