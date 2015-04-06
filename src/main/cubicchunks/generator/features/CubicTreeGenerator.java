package cubicchunks.generator.features;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class CubicTreeGenerator extends CubicFeatureGenerator {
	
	private IBlockState woodBlock;
	private IBlockState leafBlock;

	public CubicTreeGenerator(final boolean updateNeighbors, final IBlockState woodBlock, final IBlockState leafBlock) {
		super(updateNeighbors);
		this.woodBlock = woodBlock;
		this.leafBlock = leafBlock;
	}
	
	@Override
	public abstract boolean generate(World world, Random rand, BlockPos pos);
	
	protected boolean canReplaceBlock(final Block blockToCheck) {
		return testMaterialsForReplacement(blockToCheck) || testBlocksForReplacement(blockToCheck);
	}
	
	protected boolean tryToPlaceDirtUnderTree(final World world, final BlockPos blockPos) {
		if(world.getBlockStateAt(blockPos).getBlock() != Blocks.DIRT) {
			return setBlock(world, blockPos, Blocks.DIRT.getDefaultState());
		} else {
			// it's already dirt, so just say it was placed successfully
			return true;
		}
	}
	
	private boolean testMaterialsForReplacement(final Block blockToCheck) {
		final Material blockMaterial = blockToCheck.getMaterial();
		return blockMaterial == Material.AIR
				|| blockMaterial == Material.LEAVES;
	}
	
	private boolean testBlocksForReplacement(final Block blockToCheck) {
		return blockToCheck == Blocks.GRASS
				|| blockToCheck == Blocks.DIRT
				|| blockToCheck == Blocks.LOG
				|| blockToCheck == Blocks.LOG2
				|| blockToCheck == Blocks.SAPLING
				|| blockToCheck == Blocks.VINE;
	}

	public IBlockState getWoodBlock() {
		return woodBlock;
	}

	public IBlockState getLeafBlock() {
		return leafBlock;
	}

}
