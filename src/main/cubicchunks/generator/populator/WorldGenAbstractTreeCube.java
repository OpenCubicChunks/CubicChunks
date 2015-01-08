package cuchaz.cubicChunks.generator.populator;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public abstract class WorldGenAbstractTreeCube extends WorldGeneratorCube
{
	public WorldGenAbstractTreeCube( boolean flag )
	{
		super( flag );
	}

	public void afterGenerate( World world, Random rand, int x, int y, int z )
	{
	}

	protected boolean isReplacableFromTreeGen( Block block )
	{
		return block.getMaterial() == Material.air || block.getMaterial() == Material.leaves || block == Blocks.grass || block == Blocks.dirt || block == Blocks.log || block == Blocks.log2 || block == Blocks.sapling || block == Blocks.vine;
	}
}
