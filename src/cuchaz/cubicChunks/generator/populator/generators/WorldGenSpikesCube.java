package cuchaz.cubicChunks.generator.populator.generators;

import cuchaz.cubicChunks.generator.populator.WorldGeneratorCube;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class WorldGenSpikesCube extends WorldGeneratorCube
{
	private Block block;

	public WorldGenSpikesCube( Block block )
	{
		this.block = block;
	}

	@Override
	public boolean generate( World world, Random rand, int x, int y, int z )
	{
		//This is part of The End generator.
		if( !world.isAirBlock( x, y, z ) || world.getBlock( x, y - 1, z ) != this.block )
		{
			return false;

		}

		//this is a high structure and may populate aditional cubes (or not fully generate)
		int height = rand.nextInt( 32 ) + 6;
		int radius = rand.nextInt( 4 ) + 1;

		for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
		{
			for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
			{
				int xDist = xAbs - x;
				int zDist = zAbs - z;

				if( xDist * xDist + zDist * zDist <= radius * radius + 1 && world.getBlock( xAbs, y - 1, zAbs ) != this.block )
				{
					return false;
				}
			}
		}

		for( int yAbs = y; yAbs < y + height; ++yAbs )
		{
			for( int xAbs = x - radius; xAbs <= x + radius; ++xAbs )
			{
				for( int zAbs = z - radius; zAbs <= z + radius; ++zAbs )
				{
					int xDist = xAbs - x;
					int zDist = zAbs - z;

					if( xDist * xDist + zDist * zDist <= radius * radius + 1 )
					{
						world.setBlock( xAbs, yAbs, zAbs, Blocks.obsidian, 0, 2 );
					}
				}
			}
		}

		EntityEnderCrystal enderCrystal = new EntityEnderCrystal( world );
		enderCrystal.setLocationAndAngles( x + 0.5F, y + height, z + 0.5F, rand.nextFloat() * 360.0F, 0.0F );
		world.spawnEntityInWorld( enderCrystal );
		world.setBlock( x, y + height, z, Blocks.bedrock, 0, 2 );
		return true;
	}
}
