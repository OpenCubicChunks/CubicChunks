package cubicchunks.generator.features;

import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public abstract class CubicFeatureGenerator {

	private final boolean updateNeighbors;
	
	protected static int getMinCubeY(final int y) {
		int minY = (y >> 4) << 4;
		return minY;
	}
	
	/**
	 * The Constructor to use for world generation.
	 */
	public CubicFeatureGenerator() {
		this(false);
	}
	
	/**
	 * 
	 * @param updateNeighbors Set to true if you want to update the blocks neighbors. Should be false during world generation.
	 */
	public CubicFeatureGenerator(boolean updateNeighbors) {
		this.updateNeighbors = updateNeighbors;
	}
	
	/**
	 * Generate the feature in the specified world, at the specified position.
	 * 
	 * @param world The world to place the feature in.
	 * @param rand The RNG to use.
	 * @param pos The location to place the feature at.
	 * @return Returns true if the block was successfully placed, false if not.
	 */
	public abstract boolean generate(final World world, final Random rand, final BlockPos pos);
	
	public boolean setBlock(final World world, final BlockPos blockPos, final IBlockState blockState) {
		if(this.updateNeighbors) {
			return setBlockAndUpdateNeighbors(world, blockPos, blockState);
		} else {
			return setBlockOnly(world, blockPos, blockState);
		}
	}

	private boolean setBlockAndUpdateNeighbors(final World world, final BlockPos blockPos, final IBlockState blockState) {
		return world.tryPlaceBlock(blockPos, blockState, 3);
		
	}
	
	protected boolean setBlockOnly(final World world, final BlockPos blockPos, final IBlockState blockState) {
		return world.tryPlaceBlock(blockPos, blockState, 2);
	}
	
}
