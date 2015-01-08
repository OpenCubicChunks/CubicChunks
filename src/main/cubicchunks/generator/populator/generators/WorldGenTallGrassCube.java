package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenTallGrass;

public class WorldGenTallGrassCube extends WorldGeneratorCube
{

	private final Block block;
	private final int meta;

	public WorldGenTallGrassCube( Block block, int meta )
	{
		this.block = block;
		this.meta = meta;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		Block block_temp;

		int minY = getMinBlockYFromRandY( y );

		//Find top block (if not below minY)
		while( ((block_temp = world.getBlock( x, y, z )).getMaterial() == Material.air || block_temp.getMaterial() == Material.leaves) )
		{
			//Do not generate if we reach minY without finding any solid blocks.
			if( --y < minY )
			{
				return false;
			}
		}

		for( int i = 0; i < 128; ++i )
		{
			int xGen = x + rand.nextInt( 8 ) - rand.nextInt( 8 );
			int yGen = y + rand.nextInt( 4 ) - rand.nextInt( 4 );
			int zGen = z + rand.nextInt( 8 ) - rand.nextInt( 8 );

			if( world.isAirBlock( xGen, yGen, zGen ) && this.block.canBlockStay( world, xGen, yGen, zGen ) )
			{
				world.setBlock( xGen, yGen, zGen, this.block, this.meta, 2 );
			}
		}

		return true;
	}
}
