package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGenAbstractTreeCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

public class WorldGenCanopyTreeCube extends WorldGenAbstractTreeCube
{
	public WorldGenCanopyTreeCube( boolean duUpdateNotify )
	{
		super( duUpdateNotify );
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int height = rand.nextInt( 3 ) + rand.nextInt( 2 ) + 6;
		boolean canGenerate = true;

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

					//canReplace(...)
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
		this.setBlock( world, x + 1, y - 1, z, Blocks.dirt );
		this.setBlock( world, x + 1, y - 1, z + 1, Blocks.dirt );
		this.setBlock( world, x, y - 1, z + 1, Blocks.dirt );
		int var21 = rand.nextInt( 4 );
		int height2 = height - rand.nextInt( 4 );
		int xzOffsetSteps = 2 - rand.nextInt( 3 );
		int xAbs = x;
		int zAbs = z;
		int lastYAbs = 0;

		for( int yRel = 0; yRel < height; ++yRel )
		{
			int yAbs = y + yRel;

			if( yRel >= height2 && xzOffsetSteps > 0 )
			{
				xAbs += Direction.offsetX[var21];
				zAbs += Direction.offsetZ[var21];
				--xzOffsetSteps;
			}

			Block block = world.getBlock( xAbs, yAbs, zAbs );

			if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
			{
				this.setBlock( world, xAbs, yAbs, zAbs, Blocks.log2, 1 );
				this.setBlock( world, xAbs + 1, yAbs, zAbs, Blocks.log2, 1 );
				this.setBlock( world, xAbs, yAbs, zAbs + 1, Blocks.log2, 1 );
				this.setBlock( world, xAbs + 1, yAbs, zAbs + 1, Blocks.log2, 1 );
				lastYAbs = yAbs;
			}
		}

		for( int xRel = -2; xRel <= 0; ++xRel )
		{
			for( int zRel = -2; zRel <= 0; ++zRel )
			{
				byte yRel = -1;
				this.setBlockIfAir( world, xAbs + xRel, lastYAbs + yRel, zAbs + zRel );
				this.setBlockIfAir( world, 1 + xAbs - xRel, lastYAbs + yRel, zAbs + zRel );
				this.setBlockIfAir( world, xAbs + xRel, lastYAbs + yRel, 1 + zAbs - zRel );
				this.setBlockIfAir( world, 1 + xAbs - xRel, lastYAbs + yRel, 1 + zAbs - zRel );

				if( (xRel > -2 || zRel > -1) && (xRel != -1 || zRel != -2) )
				{
					yRel = 1;
					this.setBlockIfAir( world, xAbs + xRel, lastYAbs + yRel, zAbs + zRel );
					this.setBlockIfAir( world, 1 + xAbs - xRel, lastYAbs + yRel, zAbs + zRel );
					this.setBlockIfAir( world, xAbs + xRel, lastYAbs + yRel, 1 + zAbs - zRel );
					this.setBlockIfAir( world, 1 + xAbs - xRel, lastYAbs + yRel, 1 + zAbs - zRel );
				}
			}
		}

		if( rand.nextBoolean() )
		{
			this.setBlockIfAir( world, xAbs, lastYAbs + 2, zAbs );
			this.setBlockIfAir( world, xAbs + 1, lastYAbs + 2, zAbs );
			this.setBlockIfAir( world, xAbs + 1, lastYAbs + 2, zAbs + 1 );
			this.setBlockIfAir( world, xAbs, lastYAbs + 2, zAbs + 1 );
		}

		for( int xRel = -3; xRel <= 4; ++xRel )
		{
			for( int zRel = -3; zRel <= 4; ++zRel )
			{
				if( (xRel != -3 || zRel != -3) && (xRel != -3 || zRel != 4) && (xRel != 4 || zRel != -3) && (xRel != 4 || zRel != 4) && (Math.abs( xRel ) < 3 || Math.abs( zRel ) < 3) )
				{
					this.setBlockIfAir( world, xAbs + xRel, lastYAbs, zAbs + zRel );
				}
			}
		}

		for( int xRel = -1; xRel <= 2; ++xRel )
		{
			for( int zRel = -1; zRel <= 2; ++zRel )
			{
				if( (xRel < 0 || xRel > 1 || zRel < 0 || zRel > 1) && rand.nextInt( 3 ) <= 0 )
				{
					int randHeight = rand.nextInt( 3 ) + 2;

					for( int yRel = 0; yRel < randHeight; ++yRel )
					{
						this.setBlock( world, x + xRel, lastYAbs - yRel - 1, z + zRel, Blocks.log2, 1 );
					}

					for( int xRel2 = -1; xRel2 <= 1; ++xRel2 )
					{
						for( int zRel2 = -1; zRel2 <= 1; ++zRel2 )
						{
							this.setBlockIfAir( world, xAbs + xRel + xRel2, lastYAbs - 0, zAbs + zRel + zRel2 );
						}
					}

					for( int xRel2 = -2; xRel2 <= 2; ++xRel2 )
					{
						for( int zRel2 = -2; zRel2 <= 2; ++zRel2 )
						{
							if( Math.abs( xRel2 ) != 2 || Math.abs( zRel2 ) != 2 )
							{
								this.setBlockIfAir( world, xAbs + xRel + xRel2, lastYAbs - 1, zAbs + zRel + zRel2 );
							}
						}
					}
				}
			}
		}

		return true;
	}

	private void setBlockIfAir( World world, int x, int y, int z )
	{
		Block block = world.getBlock( x, y, z );

		if( block.getMaterial() == Material.air )
		{
			this.setBlock( world, x, y, z, Blocks.leaves2, 1 );
		}
	}
}
