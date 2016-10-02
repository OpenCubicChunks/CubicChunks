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
package cubicchunks.worldgen.generator;

import java.util.List;

import cubicchunks.world.cube.Cube;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.biome.Biome;

public interface ICubeGenerator {
	
	static final Vec3i[] RECOMENDED_POPULATOR_REQUIRMENT = new Vec3i[]{
					new Vec3i(-1, -2, -1), // give an extra 16 blocks virtical buffer for things like jungle trees
					new Vec3i(0, 0, 0)
			};
	
	/**
	 * Generates a new cube
	 * 
	 * @param cubeX the cube's X coordinate
	 * @param cubeY the cube's Y coordinate
	 * @param cubeZ the cube's Z coordinate
	 * @return A new CubePrimer
	 */
	CubePrimer generateCube(int cubeX, int cubeY, int cubeZ);
	
	/**
	 * Populates a Cube with trees, ores, and other
	 * multi-block structures that can cross cube boundaries.
	 * Population should* be done with the restriction that it may not
	 * effect cubes who's call to {@link IChunkGenerator#getPopRequirement(Cube)}
	 * would not include {@code cube} as an effect.<br> 
	 * <br>
	 * *Note: Unlike vanilla this method will NEVER cause recursive generation,
	 * thus the area that it populates is not as strict, but you still try not to
	 * go over it as the player might see something
	 * generate in a chunk they have already been sent
	 * 
	 * @param cube the cube to populate
	 */
	void populate(Cube cube);
	
	/**
	 * Gets a bounding box defining a range of Cube's whos population contributes to {@cube cube}
	 * having complete population.<br>
	 * example: a call to {@link ICubeGenerator#populate(Cube)}
	 * populates an 16x16x16 area centered in a 2x2x2 area of Cubes
	 * no matter what Cube is being populated. This method would return {(-1,-1,-1), (0, 0, 0)}
	 * indicating that populate calls to all the Cubes in that area contribute to cube.<br>
	 * <br>
	 * This allows a generator to have a much more dynamic populator,
	 * note however that large ranges are not recommended. If you need to generate a large structure like a nether fort,
	 * do not use a populator.
	 * @return an array of 2 Vec3i's defining a bounding box in cube coords
	 */
	//TODO: change the return type to something better
	Vec3i[] getPopRequirment(Cube cube);
	
	/**
	 * Called to reload structures that apply to {@code cube}.
	 * Mostly used to get ready for calls to {@link ICubeGenerator#getPossibleCreatures(EnumCreatureType, BlockPos))}
	 * <br>
	 * Note: if your generator works better on a 2D system you can do your 2D
	 * loading in {@link IColumnGenerator#recreateStructures(Column)}
	 * 
	 * @param cube The cube that is being loaded
	 */
	void recreateStructures(Cube cube);
	
	/**
	 * Gets a list of entitys that can spawn at pos...
	 * Used for things like skeletons and blazes spawning in nether forts.<br>
	 * <br>
	 * Note: {@link IColumnGenerator#getPossibleCreatures()} will take priority over this
	 * 
	 * @param creatureType the creature type that we are interested in getting
	 * @param pos the block position where we need to see what entitys can spawn at
	 * @return a list of mobs that can spawn (example: nether forts return, EntityBlaze, EntityPigZombie, EntitySkeleton, EntityMagmaCube)
	 */
	List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos);
}
