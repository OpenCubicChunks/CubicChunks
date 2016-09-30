package cubicchunks.world.cube;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;

@SuppressWarnings("deprecation") // Block.BLOCK_STATE_IDS
public class CubePrimer implements ICubePrimer{
	private static final IBlockState DEFAULT_STATE = Blocks.AIR.getDefaultState();
	private final char[] data = new char[4096];

	public IBlockState getBlockState(int x, int y, int z) {
		IBlockState iblockstate = Block.BLOCK_STATE_IDS.getByValue(this.data[getBlockIndex(x, y, z)]);
		return iblockstate == null ? DEFAULT_STATE : iblockstate;
	}

	public void setBlockState(int x, int y, int z, IBlockState state) {
		this.data[getBlockIndex(x, y, z)] = (char) Block.BLOCK_STATE_IDS.get(state);
	}

	private static int getBlockIndex(int x, int y, int z) {
		return x << 8 | z << 4 | y;
	}

	public int findGroundHeight(int x, int z) {
		int i = (x << 8 | z << 4) + 15;

		for (int j = 15; j >= 0; --j) {
			IBlockState iblockstate = Block.BLOCK_STATE_IDS.getByValue(this.data[i + j]);

			if (iblockstate != null && iblockstate != DEFAULT_STATE) {
				return j;
			}
		}

		return -1; // no non-air block found
	}
}
