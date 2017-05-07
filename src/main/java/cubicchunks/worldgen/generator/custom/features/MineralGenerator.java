/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.worldgen.generator.custom.features;

import cubicchunks.util.CubePos;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.GlobalGeneratorConfig;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.feature.WorldGenMinable;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MineralGenerator extends FeatureGenerator {

    private final double minY;
    private final double maxY;

    private final double probability;
    private final int numberOfBlocks;
    private final int seed;
    private final IBlockState oreBlock;
    private final Predicate<IBlockState> predicate;

    /**
     * Creates new OreGenerator with given min/max height, vein size and number of generation attempts.
     * <p>
     * minY and maxY: <ul> <li>-1 - seaLevel-maxTerrainHeight <li>0 - sea level. <li>1 - seaLevel+maxTerrainHeight
     * </ul>
     *
     * @param minY Minimum generation height
     * @param maxY Maximum generation height
     * @param size Maximum vein size
     */
    public MineralGenerator(final ICubicWorld world, final IBlockState state, final double minY, final double maxY,
            final int size, final double probability, final int seedIn) {
        super(world);
        this.seed=seedIn;
        this.numberOfBlocks=size;
        this.oreBlock=state;
        this.predicate=new MineralGenerator.StonePredicate();
        this.minY = minY;
        this.maxY = maxY;
        this.probability = probability;
    }

    @Override
    public void generate(final Random rand, final Cube cube, final Biome biome) {
        BlockPos cubeStart = cube.getCoords().getMinBlockPos();
        CubePos cpos = cube.getCoords();
        double maxBlockY = this.maxY * GlobalGeneratorConfig.MAX_ELEV + GlobalGeneratorConfig.SEA_LEVEL;
        double minBlockY = this.minY * GlobalGeneratorConfig.MAX_ELEV + GlobalGeneratorConfig.SEA_LEVEL;
        if (cubeStart.getY() <= maxBlockY && cubeStart.getY() >= minBlockY) {
        	Set<BlockPos> pointsOfInterest = new HashSet<BlockPos>();
        	cpos.forEachWithinRange(1, cubePos -> {
        		rand.setSeed(seed<<16^world.getSeed()<<12^cubePos.getX()<<8^cubePos.getY()<<4^cubePos.getZ());
                if (rand.nextDouble() > this.probability) {
                    return;
                }
                int x = cubePos.getMinBlockX()+rand.nextInt(16);
                int y = cubePos.getMinBlockY()+rand.nextInt(16);
                int z = cubePos.getMinBlockZ()+rand.nextInt(16);
                pointsOfInterest.add(new BlockPos(x,y,z));
        	});
			ExtendedBlockStorage cstorage = cube.getStorage();        	
			for (int lx = 0; lx < 16; lx++)
				for (int ly = 0; ly < 16; ly++)
					for (int lz = 0; lz < 16; lz++) {
						if(predicate.apply(cstorage.get(lx, ly, lz))) {
							for (BlockPos pointOfInterest : pointsOfInterest) {
								int dx = pointOfInterest.getX() - cubeStart.getX() - lx;
								int dy = pointOfInterest.getY() - cubeStart.getY() - ly;
								int dz = pointOfInterest.getZ() - cubeStart.getZ() - lz;
								if (dx * dx + dy * dy + dz * dz < this.numberOfBlocks) {
									cstorage.set(lx, ly, lz, oreBlock);
									break;
								}
							}
						}
					}				
        }
    }
    
	static class StonePredicate implements Predicate<IBlockState> {
		private StonePredicate() {
		}

		public boolean apply(IBlockState p_apply_1_) {
			if (p_apply_1_ != null && p_apply_1_.getBlock() == Blocks.STONE) {
				BlockStone.EnumType blockstone$enumtype = (BlockStone.EnumType) p_apply_1_.getValue(BlockStone.VARIANT);
				return blockstone$enumtype.isNatural();
			} else {
				return false;
			}
		}
	}
}
