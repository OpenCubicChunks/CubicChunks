package cubicchunks.generator.features;

import java.util.Random;

import cubicchunks.world.cube.Cube;
import net.minecraft.block.Block;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

public class TallGrassGenerator extends SurfaceFeatureGenerator {
	
	private IBlockState tallGrassBlockState;
	
	public TallGrassGenerator(final World world, final BlockTallGrass.TallGrassTypes tallGrassType) {
		super(world);
		this.tallGrassBlockState = Blocks.TALLGRASS.getDefaultState().setProperty(BlockTallGrass.type, tallGrassType);
	}

	@Override
	public int getAttempts(Random rand, Biome biome) {
		return biome.biomeDecorator.randomGrassPerChunk;
	}
	
	@Override
	public void generateAt(final Random rand, final BlockPos pos, final Biome biome) {
		Block currentBlock;
		BlockPos currentPos = pos;
		
		final int minY = getMinCubeY(pos.getY());
		
		while((currentBlock = world.getBlockStateAt(currentPos).getBlock()).getMaterial() == Material.AIR || currentBlock.getMaterial() == Material.LEAVES) {
			currentPos = currentPos.below();
			
			if(currentPos.getY() < minY) {
				return;
			}
		}
		
		for(int i = 0; i < 128; ++i) {
			BlockPos randomPos = currentPos.add(rand.nextInt(8) - rand.nextInt(8),
					rand.nextInt(4) - rand.nextInt(4),
					rand.nextInt(8) - rand.nextInt(8));
			
			if(world.hasAirAt(randomPos) && Blocks.TALLGRASS.canBePlacedAt(world, randomPos, tallGrassBlockState)) {
				this.setBlockOnly(randomPos, tallGrassBlockState);
			}	
		}
	}
}
