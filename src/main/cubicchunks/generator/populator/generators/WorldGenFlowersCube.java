package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public class WorldGenFlowersCube extends WorldGeneratorCube
{
	private Block blockType;
	private int metadata;

	public WorldGenFlowersCube( Block blockType )
	{
		this.blockType = blockType;
	}

	public void setTypeMetadata( Block blockType, int blockMetadata )
	{
		this.blockType = blockType;
		this.metadata = blockMetadata;
	}
	
	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		for( int i = 0; i < 64; ++i )
		{
			int xAbs = x + rand.nextInt( 8 ) - rand.nextInt( 8 );
			int yAbs = y + rand.nextInt( 4 ) - rand.nextInt( 4 );
			int zAbs = z + rand.nextInt( 8 ) - rand.nextInt( 8 );

			if( world.isAirBlock( xAbs, yAbs, zAbs ) && (!world.provider.hasNoSky) && this.blockType.canBlockStay( world, xAbs, yAbs, zAbs ) )
			{
				world.setBlock( xAbs, yAbs, zAbs, this.blockType, this.metadata, 2 );
			}
		}

		return true;
	}
}
