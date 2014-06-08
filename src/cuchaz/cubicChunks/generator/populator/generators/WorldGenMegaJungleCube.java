package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGenHugeTreesCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class WorldGenMegaJungleCube extends WorldGenHugeTreesCube
{
	public WorldGenMegaJungleCube( boolean flag, int baseHeight, int maxRandAdditionalHeight, int woodMeta, int leavesMeta )
	{
		super( flag, baseHeight, maxRandAdditionalHeight, woodMeta, leavesMeta );
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		int treeHeight = this.getRandomHeight( rand );

		if( !this.canGenerate( world, rand, x, y, z, treeHeight ) )
		{
			return false;
		}
		
		this.generateTreeBase( world, x, z, y + treeHeight, 2, rand );

		for( int yAbs = y + treeHeight - 2 - rand.nextInt( 4 ); yAbs > y + treeHeight / 2; yAbs -= 2 + rand.nextInt( 4 ) )
		{
			float angle = rand.nextFloat() * (float)Math.PI * 2.0F;
			int xAbs = x + (int)(0.5F + MathHelper.cos( angle ) * 4.0F);
			int zAbs = z + (int)(0.5F + MathHelper.sin( angle ) * 4.0F);
			int yRel;

			for( yRel = 0; yRel < 5; ++yRel )
			{
				xAbs = x + (int)(1.5F + MathHelper.cos( angle ) * yRel);
				zAbs = z + (int)(1.5F + MathHelper.sin( angle ) * yRel);
				this.setBlock( world, xAbs, yAbs - 3 + yRel / 2, zAbs, Blocks.log, this.woodMetadata );
			}

			yRel = 1 + rand.nextInt( 2 );
			int maxY = yAbs;

			for( int yGen = yAbs - yRel; yGen <= maxY; ++yGen )
			{
				int var14 = yGen - maxY;
				this.generateLayerAtLocationWithRadius( world, xAbs, yGen, zAbs, 1 - var14, rand );
			}
		}

		for( int yRel = 0; yRel < treeHeight; ++yRel )
		{
			Block block = world.getBlock( x, y + yRel, z );

			if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
			{
				this.setBlock( world, x, y + yRel, z, Blocks.log, this.woodMetadata );

				if( yRel > 0 )
				{
					if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x - 1, y + yRel, z ) )
					{
						this.setBlock( world, x - 1, y + yRel, z, Blocks.vine, 8 );
					}

					if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x, y + yRel, z - 1 ) )
					{
						this.setBlock( world, x, y + yRel, z - 1, Blocks.vine, 1 );
					}
				}
			}

			if( yRel < treeHeight - 1 )
			{
				block = world.getBlock( x + 1, y + yRel, z );

				if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
				{
					this.setBlock( world, x + 1, y + yRel, z, Blocks.log, this.woodMetadata );

					if( yRel > 0 )
					{
						if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x + 2, y + yRel, z ) )
						{
							this.setBlock( world, x + 2, y + yRel, z, Blocks.vine, 2 );
						}

						if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x + 1, y + yRel, z - 1 ) )
						{
							this.setBlock( world, x + 1, y + yRel, z - 1, Blocks.vine, 1 );
						}
					}
				}

				block = world.getBlock( x + 1, y + yRel, z + 1 );

				if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
				{
					this.setBlock( world, x + 1, y + yRel, z + 1, Blocks.log, this.woodMetadata );

					if( yRel > 0 )
					{
						if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x + 2, y + yRel, z + 1 ) )
						{
							this.setBlock( world, x + 2, y + yRel, z + 1, Blocks.vine, 2 );
						}

						if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x + 1, y + yRel, z + 2 ) )
						{
							this.setBlock( world, x + 1, y + yRel, z + 2, Blocks.vine, 4 );
						}
					}
				}

				block = world.getBlock( x, y + yRel, z + 1 );

				if( block.getMaterial() == Material.air || block.getMaterial() == Material.leaves )
				{
					this.setBlock( world, x, y + yRel, z + 1, Blocks.log, this.woodMetadata );

					if( yRel > 0 )
					{
						if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x - 1, y + yRel, z + 1 ) )
						{
							this.setBlock( world, x - 1, y + yRel, z + 1, Blocks.vine, 8 );
						}

						if( rand.nextInt( 3 ) > 0 && world.isAirBlock( x, y + yRel, z + 2 ) )
						{
							this.setBlock( world, x, y + yRel, z + 2, Blocks.vine, 4 );
						}
					}
				}
			}
		}

		return true;
	}

	private void generateTreeBase( World world, int x, int z, int maxY, int radiusBase, Random rand )
	{
		byte b1 = 2;

		for( int y = maxY - b1; y <= maxY; ++y )
		{
			int yRel = y - maxY;
			this.generateLayerAtLocationWithRadius2x2( world, x, y, z, radiusBase + 1 - yRel, rand );
		}
	}
}
