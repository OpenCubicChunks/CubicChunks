package cuchaz.cubicChunks.generator.populator;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;

public abstract class WorldGeneratorCube extends WorldGenerator
{
	protected static int getMinBlockYFromRandY( int y )
	{
		//we get:
		//y = multiplyOf16 + 8 + randomValue

		//we want to get:
		//minY = multiplyOf16 + 8
		//
		//RandomValue is 0-15
		//
		//Substract 8. After this minY = multiplyOf16 + randomValue
		//int minY = y - 8;
		//
		//Remove randomValue (4 bits). After this minY = multiplyOf16
		//minY &= 0xFFFFFFF0;
		//
		//add 8 to get minY
		//minY += 8;
		
		int minY = ((y - 8) & 0xFFFFFFF0) + 8;
		assert y - minY >= 0 && y - minY < 16;
		return minY;
	}

	public WorldGeneratorCube()
	{
		super();
	}

	public WorldGeneratorCube( boolean doBlockNotify )
	{
		super( doBlockNotify );
	}

	//deobfuscated method names
	protected void setBlock( World world, int x, int y, int z, Block block )
	{
		this.func_150515_a( world, x, y, z, block );
	}

	protected void setBlock( World world, int x, int y, int z, Block block, int metadata )
	{
		this.func_150516_a( world, x, y, z, block, metadata );
	}
}
