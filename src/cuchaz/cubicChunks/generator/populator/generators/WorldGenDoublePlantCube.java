package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenDoublePlantCube extends WorldGeneratorCube
{
	private int metadata;

	public void setType( int metadata )
	{
		this.metadata = metadata;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		boolean generated = false;

		for( int i = 0; i < 64; ++i )
		{
			int xAbs = x + rand.nextInt( 8 ) - rand.nextInt( 8 );
			int yAbs = y + rand.nextInt( 4 ) - rand.nextInt( 4 );
			int zAbs = z + rand.nextInt( 8 ) - rand.nextInt( 8 );

			if( world.isAirBlock( xAbs, yAbs, zAbs ) && (!world.provider.hasNoSky) && Blocks.double_plant.canPlaceBlockAt( world, xAbs, yAbs, zAbs ) )
			{
				Blocks.double_plant.func_149889_c( world, xAbs, yAbs, zAbs, this.metadata, 2 );
				generated = true;
			}
		}

		return generated;
	}
}
