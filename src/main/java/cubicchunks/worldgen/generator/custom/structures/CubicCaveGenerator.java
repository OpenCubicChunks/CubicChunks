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
package cubicchunks.worldgen.generator.custom.structures;

import cubicchunks.util.CubePos;
import cubicchunks.util.StructureGenUtil;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.generator.ICubePrimer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Random;
import java.util.function.Predicate;

import static cubicchunks.util.Coords.cubeToMinBlock;
import static cubicchunks.util.Coords.localToBlock;
import static cubicchunks.util.StructureGenUtil.normalizedDistance;
import static cubicchunks.util.StructureGenUtil.scanWallsForBlock;
import static java.lang.Math.max;
import static net.minecraft.util.math.MathHelper.cos;
import static net.minecraft.util.math.MathHelper.floor_double;
import static net.minecraft.util.math.MathHelper.sin;

/*
 * Modified Minecraft cave generation code. Based on Robinton's cave generation implementation.
 */
//TODO: Fix code duplication beterrn cave and cave generators
public class CubicCaveGenerator extends CubicStructureGenerator {

	//=============================================
	//Possibly configurable values
	//=============================================

	/**
	 * 1 in CAVE_RARITY attempts will result in generating any caves at all
	 */
	private static final int CAVE_RARITY = 16*7;

	/**
	 * Maximum amount of starting nodes
	 */
	private static final int MAX_INIT_NODES = 14;

	/**
	 * 1 in LARGE_NODE_RARITY initial attempts will result in large node
	 */
	private static final int LARGE_NODE_RARITY = 4;

	/**
	 * The maximum amount of additional branches after generating large node.
	 * Random value between 0 and LARGE_NODE_MAX_BRANCHES is chosen.
	 */
	private static final int LARGE_NODE_MAX_BRANCHES = 4;

	/**
	 * 1 in BIG_CAVE_RARITY branches will start bigger than usual
	 */
	private static final int BIG_CAVE_RARITY = 10;

	/**
	 * Value added to the size of the cave (radius)
	 */
	private static final double CAVE_SIZE_ADD = 1.5D;

	/**
	 * In 1 of STEEP_STEP_RARITY steps, cave will be flattened using
	 * STEEPER_FLATTEN_FACTOR instead of FLATTEN_FACTOR
	 */
	private static final int STEEP_STEP_RARITY = 6;

	/**
	 * After each step the Y direction component will be multiplied by this value,
	 * unless steeper cave is allowed
	 */
	private static final double FLATTEN_FACTOR = 0.7;

	/**
	 * If steeper cave is allowed - this value will be used instead of FLATTEN_FACTOR
	 */
	private static final double STEEPER_FLATTEN_FACTOR = 0.92;

	/**
	 * Each step cave direction angles will be changed by this fraction
	 * of values that specify how direction changes
	 */
	private static final double DIRECTION_CHANGE_FACTOR = 0.1;

	/**
	 * This fraction of the previous value that controls horizontal direction changes will be used in next step
	 */
	private static final double PREV_HORIZ_DIRECTION_CHANGE_WEIGHT = 0.75;

	/**
	 * This fraction of the previous value that controls vertical direction changes will be used in next step
	 */
	private static final double PREV_VERT_DIRECTION_CHANGE_WEIGHT = 0.9;

	/**
	 * Maximum value by which horizontal cave direction randomly changes each step, lower values are much more likely.
	 */
	private static final double MAX_ADD_DIRECTION_CHANGE_HORIZ = 4.0;

	/**
	 * Maximum value by which vertical cave direction randomly changes each step, lower values are much more likely.
	 */
	private static final double MAX_ADD_DIRECTION_CHANGE_VERT = 2.0;

	/**
	 * 1 in this amount of steps will actually carve any blocks,
	 */
	private static final int CARVE_STEP_RARITY = 4;

	/**
	 * Relative "height" if depth floor
	 * <p>
	 * -1 results in round cave without flat floor
	 * 1 will completely fill the cave
	 * 0 will result in lower half of the cave to be filled with stone
	 */
	private static final double CAVE_FLOOR_DEPTH = -0.7;

	/**
	 * Controls which blocks can be replaced by cave
	 */
	private static final Predicate<IBlockState> isBlockReplaceable = (state ->
			state.getBlock() == Blocks.STONE || state.getBlock() == Blocks.DIRT || state.getBlock() == Blocks.GRASS);


	@Override
	protected void generate(ICubicWorld world, ICubePrimer cube,
	                        int cubeXOrigin, int cubeYOrigin, int cubeZOrigin, CubePos generatedCubePos) {
		if (this.rand.nextInt(CAVE_RARITY) != 0) {
			return;
		}
		//very low probability of generating high number
		int nodes = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(MAX_INIT_NODES + 1) + 1) + 1);

		for (int node = 0; node < nodes; ++node) {
			double branchStartX = localToBlock(cubeXOrigin, this.rand.nextInt(Cube.SIZE));
			double branchStartY = localToBlock(cubeYOrigin, this.rand.nextInt(Cube.SIZE));
			double branchStartZ = localToBlock(cubeZOrigin, this.rand.nextInt(Cube.SIZE));
			int subBranches = 1;

			if (this.rand.nextInt(LARGE_NODE_RARITY) == 0) {
				this.generateLargeNode(cube, this.rand.nextLong(), generatedCubePos,
						branchStartX, branchStartY, branchStartZ);
				subBranches += this.rand.nextInt(LARGE_NODE_MAX_BRANCHES);
			}

			for (int branch = 0; branch < subBranches; ++branch) {
				float horizDirAngle = this.rand.nextFloat()*(float) Math.PI*2.0F;
				float vertDirAngle = (this.rand.nextFloat() - 0.5F)*2.0F/8.0F;
				float baseHorizSize = this.rand.nextFloat()*2.0F + this.rand.nextFloat();

				if (this.rand.nextInt(BIG_CAVE_RARITY) == 0) {
					baseHorizSize *= this.rand.nextFloat()*this.rand.nextFloat()*3.0F + 1.0F;
				}

				int startWalkedDistance = 0;
				int maxWalkedDistance = 0;
				double vertCaveSizeMod = 1.0;

				this.generateNode(cube, this.rand.nextLong(), generatedCubePos,
						branchStartX, branchStartY, branchStartZ,
						baseHorizSize, horizDirAngle, vertDirAngle,
						startWalkedDistance, maxWalkedDistance, vertCaveSizeMod);
			}
		}
	}

	/**
	 * Generates a flattened cave "room", usually more caves split off it
	 */
	protected void generateLargeNode(ICubePrimer cube, long seed, CubePos generatedCubePos,
	                                 double x, double y, double z) {
		float baseHorizSize = 1.0F + this.rand.nextFloat()*6.0F;
		float horizDirAngle = 0;
		float vertDirAngle = 0;

		int startWalkedDistance = -1;
		int maxWalkedDistance = -1;
		double vertCaveSizeMod = 0.5;
		this.generateNode(cube, seed, generatedCubePos, x, y, z,
				baseHorizSize, horizDirAngle, vertDirAngle,
				startWalkedDistance, maxWalkedDistance, vertCaveSizeMod);
	}

	/**
	 * Recursively generates a node in the current cave system tree.
	 *
	 * @param cube block buffer to modify
	 * @param seed random seed to use
	 * @param generatedCubePos position of the cube to modify
	 * @param caveX starting x coordinate of the cave
	 * @param caveY starting Y coordinate of the cave
	 * @param caveZ starting Z coordinate of the cave
	 * @param baseCaveSize initial value for cave size, size decreases as cave goes further
	 * @param horizDirAngle horizontal direction angle
	 * @param vertCaveSizeMod vertical direction angle
	 * @param startWalkedDistance the amount of steps the cave already went forwards,
	 * used in recursive step. -1 means that there will be only one step
	 * @param maxWalkedDistance maximum distance the cave can go forwards, <= 0 to use default
	 * @param vertDirAngle changes vertical size of the cave, values < 1 result in flattened caves,
	 * > 1 result in vertically stretched caves
	 */
	protected void generateNode(ICubePrimer cube, long seed,
	                            CubePos generatedCubePos,
	                            double caveX, double caveY, double caveZ,
	                            float baseCaveSize, float horizDirAngle, float vertDirAngle,
	                            int startWalkedDistance, int maxWalkedDistance, double vertCaveSizeMod) {
		Random rand = new Random(seed);

		//store by how much the horizontal and vertical direction angles will change each step
		float horizDirChange = 0.0F;
		float vertDirChange = 0.0F;

		if (maxWalkedDistance <= 0) {
			int maxBlockRadius = cubeToMinBlock(this.range - 1);
			maxWalkedDistance = maxBlockRadius - rand.nextInt(maxBlockRadius/4);
		}

		//if true - this branch won't generate new sub-branches
		boolean finalStep = false;

		int walkedDistance;
		if (startWalkedDistance == -1) {
			//generate a cave "room"
			//start at half distance towards the end = max cave size
			walkedDistance = maxWalkedDistance/2;
			finalStep = true;
		} else {
			walkedDistance = startWalkedDistance;
		}

		int splitPoint = rand.nextInt(maxWalkedDistance/2) + maxWalkedDistance/4;

		for (; walkedDistance < maxWalkedDistance; ++walkedDistance) {
			float fractionWalked = walkedDistance/(float) maxWalkedDistance;
			//horizontal and vertical size of the cave
			//size starts small and increases, then decreases as cave goes further
			double caveSizeHoriz = CAVE_SIZE_ADD + sin(fractionWalked*(float) Math.PI)*baseCaveSize;
			double caveSizeVert = caveSizeHoriz*vertCaveSizeMod;

			//Walk forward a single step:

			//from sin(alpha)=y/r and cos(alpha)=x/r ==> x = r*cos(alpha) and y = r*sin(alpha)
			//always moves by one block in some direction

			//here x is xzDirectionFactor, y is yDirectionFactor
			float xzDirectionFactor = cos(vertDirAngle);
			float yDirectionFactor = sin(vertDirAngle);

			//here y is directionZ and x is directionX
			caveX += cos(horizDirAngle)*xzDirectionFactor;
			caveY += yDirectionFactor;
			caveZ += sin(horizDirAngle)*xzDirectionFactor;

			if (rand.nextInt(STEEP_STEP_RARITY) == 0) {
				vertDirAngle *= STEEPER_FLATTEN_FACTOR;
			} else {
				vertDirAngle *= FLATTEN_FACTOR;
			}

			//change the direction
			vertDirAngle += vertDirChange*DIRECTION_CHANGE_FACTOR;
			horizDirAngle += horizDirChange*DIRECTION_CHANGE_FACTOR;
			//update direction change angles
			vertDirChange *= PREV_VERT_DIRECTION_CHANGE_WEIGHT;
			horizDirChange *= PREV_HORIZ_DIRECTION_CHANGE_WEIGHT;
			vertDirChange += (rand.nextFloat() - rand.nextFloat())*rand.nextFloat()*MAX_ADD_DIRECTION_CHANGE_VERT;
			horizDirChange += (rand.nextFloat() - rand.nextFloat())*rand.nextFloat()*MAX_ADD_DIRECTION_CHANGE_HORIZ;

			//if we reached split point - try to split
			//can split only if it's not final branch and the cave is still big enough (>1 block radius)
			if (!finalStep && walkedDistance == splitPoint && baseCaveSize > 1.0F) {
				this.generateNode(cube, rand.nextLong(),
						generatedCubePos, caveX, caveY, caveZ,
						rand.nextFloat()*0.5F + 0.5F,//base cave size
						horizDirAngle - ((float) Math.PI/2F),//horiz. angle - subtract 90 degrees
						vertDirAngle/3.0F, walkedDistance, maxWalkedDistance,
						1.0D);
				this.generateNode(cube, rand.nextLong(), generatedCubePos, caveX, caveY, caveZ,
						rand.nextFloat()*0.5F + 0.5F,//base cave size
						horizDirAngle + ((float) Math.PI/2F),//horiz. angle - add 90 degrees
						vertDirAngle/3.0F, walkedDistance, maxWalkedDistance,
						1.0D);
				return;
			}

			//carve blocks only on some percentage of steps, unless this is the final branch
			if (rand.nextInt(CARVE_STEP_RARITY) == 0 && !finalStep) {
				continue;
			}

			double xDist = caveX - generatedCubePos.getXCenter();
			double yDist = caveY - generatedCubePos.getYCenter();
			double zDist = caveZ - generatedCubePos.getZCenter();
			double maxStepsDist = maxWalkedDistance - walkedDistance;
			//CHANGE: multiply max(1, vertCaveSizeMod)
			double maxDistToCube = baseCaveSize*max(1, vertCaveSizeMod) + CAVE_SIZE_ADD + Cube.SIZE;

			//can this cube be reached at all?
			//if even after going max distance allowed by remaining steps, it's still too far - stop
			//TODO: does it make any performance difference?
			if (xDist*xDist + yDist*yDist + zDist*zDist - maxStepsDist*maxStepsDist > maxDistToCube*maxDistToCube) {
				return;
			}

			tryCarveBlocks(cube, generatedCubePos,
					caveX, caveY, caveZ,
					caveSizeHoriz, caveSizeVert);
			if (finalStep) {
				return;
			}
		}
	}

	//returns true if cave generation should be continued
	private void tryCarveBlocks(ICubePrimer cube, CubePos generatedCubePos,
	                            double caveX, double caveY, double caveZ,
	                            double caveSizeHoriz, double caveSizeVert) {
		double genCubeCenterX = generatedCubePos.getXCenter();
		double genCubeCenterY = generatedCubePos.getYCenter();
		double genCubeCenterZ = generatedCubePos.getZCenter();

		//Can current step position affect currently modified cube?
		//TODO: is multiply by 2 needed?
		if (caveX < genCubeCenterX - Cube.SIZE - caveSizeHoriz*2.0D ||
				caveY < genCubeCenterY - Cube.SIZE - caveSizeVert*2.0D ||
				caveZ < genCubeCenterZ - Cube.SIZE - caveSizeHoriz*2.0D ||
				caveX > genCubeCenterX + Cube.SIZE + caveSizeHoriz*2.0D ||
				caveY > genCubeCenterY + Cube.SIZE + caveSizeVert*2.0D ||
				caveZ > genCubeCenterZ + Cube.SIZE + caveSizeHoriz*2.0D) {
			return;
		}
		int minLocalX = floor_double(caveX - caveSizeHoriz) - generatedCubePos.getMinBlockX() - 1;
		int maxLocalX = floor_double(caveX + caveSizeHoriz) - generatedCubePos.getMinBlockX() + 1;
		int minLocalY = floor_double(caveY - caveSizeVert) - generatedCubePos.getMinBlockY() - 1;
		int maxLocalY = floor_double(caveY + caveSizeVert) - generatedCubePos.getMinBlockY() + 1;
		int minLocalZ = floor_double(caveZ - caveSizeHoriz) - generatedCubePos.getMinBlockZ() - 1;
		int maxLocalZ = floor_double(caveZ + caveSizeHoriz) - generatedCubePos.getMinBlockZ() + 1;

		//skip is if everything is outside of that cube
		if (maxLocalX <= 0 || minLocalX >= Cube.SIZE ||
				maxLocalY <= 0 || minLocalY >= Cube.SIZE ||
				maxLocalZ <= 0 || minLocalZ >= Cube.SIZE) {
			return;
		}
		StructureBoundingBox boundingBox = new StructureBoundingBox(minLocalX, minLocalY, minLocalZ, maxLocalX, maxLocalY, maxLocalZ);

		StructureGenUtil.clampBoundingBoxToLocalCube(boundingBox);

		boolean hitLiquid = scanWallsForBlock(cube, boundingBox,
				(b) -> b.getBlock() == Blocks.LAVA || b.getBlock() == Blocks.FLOWING_LAVA);

		if (!hitLiquid) {
			carveBlocks(cube, generatedCubePos, caveX, caveY, caveZ, caveSizeHoriz, caveSizeVert, boundingBox);
		}
	}

	private void carveBlocks(ICubePrimer cube,
	                         CubePos generatedCubePos,
	                         double caveX, double caveY, double caveZ,
	                         double caveSizeHoriz, double caveSizeVert,
	                         StructureBoundingBox boundingBox) {

		int generatedCubeX = generatedCubePos.getX();
		int generatedCubeY = generatedCubePos.getY();
		int generatedCubeZ = generatedCubePos.getZ();

		int minX = boundingBox.minX;
		int maxX = boundingBox.maxX;
		int minY = boundingBox.minY;
		int maxY = boundingBox.maxY;
		int minZ = boundingBox.minZ;
		int maxZ = boundingBox.maxZ;

		for (int localX = minX; localX < maxX; ++localX) {
			double distX = normalizedDistance(generatedCubeX, localX, caveX, caveSizeHoriz);

			for (int localZ = minZ; localZ < maxZ; ++localZ) {
				double distZ = normalizedDistance(generatedCubeZ, localZ, caveZ, caveSizeHoriz);

				if (distX*distX + distZ*distZ >= 1.0D) {
					continue;
				}
				for (int localY = minY; localY < maxY; ++localY) {
					double distY = normalizedDistance(generatedCubeY, localY, caveY, caveSizeVert);

					IBlockState state = cube.getBlockState(localX, localY, localZ);

					if (!isBlockReplaceable.test(state)) {
						continue;
					}

					if (shouldCarveBlock(distX, distY, distZ)) {
						// No lava generation, infinite depth. Lava will be generated differently (or not generated)
						cube.setBlockState(localX, localY, localZ, Blocks.AIR.getDefaultState());
					} else if (state.getBlock() == Blocks.DIRT) {
						//vanilla dirt-grass replacement works by scanning top-down and moving the block
						//cubic chunks needs to be a bit more hacky about it
						//instead of keeping track of the encountered grass block
						//cubic chunks replaces any dirt block (it's before population, no ore-like dirt formations yet)
						//with grass, if the block above would be deleted by this cave generator step
						double distYAbove = normalizedDistance(generatedCubeY, localY + 1, caveY, caveSizeVert);
						if (shouldCarveBlock(distX, distYAbove, distZ)) {
							cube.setBlockState(localX, localY, localZ, Blocks.GRASS.getDefaultState());
						}
					}
				}
			}
		}
	}

	private static boolean shouldCarveBlock(double distX, double distY, double distZ) {
		//distY > CAVE_FLOOR_DEPTH --> flattened floor
		return distY > CAVE_FLOOR_DEPTH && distX*distX + distY*distY + distZ*distZ < 1.0D;
	}
}