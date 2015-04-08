package cubicchunks.generator.features;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class CubicTallGrassGenerator extends CubicFeatureGenerator {
	
	private IBlockState tallGrassBlockState;
	
	public CubicTallGrassGenerator(final BlockTallGrass.TallGrassTypes tallGrassType) {
		this.tallGrassBlockState = Blocks.TALLGRASS.getDefaultState().setProperty(BlockTallGrass.type, tallGrassType);
	}

	@Override
	public boolean generate(final World world, final Random rand, final BlockPos pos) {
		Block currentBlock;
		BlockPos currentPos = pos;
		
		final int minY = getMinCubeY(pos.getY());
		
		while((currentBlock = world.getBlockStateAt(currentPos).getBlock()).getMaterial() == Material.AIR || currentBlock.getMaterial() == Material.LEAVES) {
			currentPos = currentPos.below();
			
			if(currentPos.getY() < minY) {
				return false;
			}
		}
		
		for(int i = 0; i < 128; ++i) {
			BlockPos randomPos = currentPos.add(rand.nextInt(8) - rand.nextInt(8),
					rand.nextInt(4) - rand.nextInt(4),
					rand.nextInt(8) - rand.nextInt(8));
			
			if(world.hasAirAt(randomPos) && Blocks.TALLGRASS.canBePlacedAt(world, randomPos, tallGrassBlockState)) {
				this.setBlockOnly(world, randomPos, tallGrassBlockState);
			}
			
		}

		return true;
	}

}
