package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;

public class WorldGenDeadBushCube extends WorldGeneratorCube
{
	private final Block blockType;

	public WorldGenDeadBushCube( Block blockType )
	{
		this.blockType = blockType;
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

		boolean generated = false;

		for( int i = 0; i < 4; ++i )
		{
			int xAbs = x + rand.nextInt( 8 ) - rand.nextInt( 8 );
			int yAbs = y + rand.nextInt( 4 ) - rand.nextInt( 4 );
			int zAbs = z + rand.nextInt( 8 ) - rand.nextInt( 8 );

			if( world.isAirBlock( xAbs, yAbs, zAbs ) && this.blockType.canBlockStay( world, xAbs, yAbs, zAbs ) )
			{
				world.setBlock( xAbs, yAbs, zAbs, this.blockType, 0, 2 );
				generated = true;
			}
		}

		return generated;
	}
}
