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

import cubicchunks.util.Box;
import cubicchunks.world.column.IColumn;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeGenerator {

    Box RECOMMENDED_POPULATOR_REQUIREMENT = new Box(
            -1, -2, -1, // give an extra 16 blocks vertical buffer for things like jungle trees
            0, 0, 0
    );

    Box NO_POPULATOR_REQUIREMENT = new Box(
            0, 0, 0,
            0, 0, 0
    );

    /**
     * Generate a new cube
     *
     * @param cubeX the cube's X coordinate
     * @param cubeY the cube's Y coordinate
     * @param cubeZ the cube's Z coordinate
     *
     * @return An ICubePrimer with the generated blocks
     */
    ICubePrimer generateCube(int cubeX, int cubeY, int cubeZ);

    /**
     * Generate column-global information such as biome data
     *
     * @param column the target column
     */
    void generateColumn(IColumn column);

    /**
     * Populate a cube with multi-block structures that can cross cube boundaries such as trees and ore veins.
     * Population should* be done with the restriction that it may not affect cubes whose call to
     * {@link ICubeGenerator#getPopulationRequirement(Cube)} would does include {@code cube}.
     *
     * Note: Unlike vanilla this method will NEVER cause recursive generation, thus the area that it populates is not as strict.
     * Generation should still be restricted as the player might see something generate in a chunk they have already been sent
     *
     * @param cube the cube to populate
     */
    void populate(Cube cube);

    /**
     * Get the bounding box defining a range of cubes whose population contributes to {@code cube} being fully
     * populated.<br> Consider the following example: A call to some implementation of
     * {@link ICubeGenerator#populate(Cube)} populates a 16x16x16 block area in a 2x2x2 area of cubes around the center.
     * <pre>
     * +----------+----------+  . .  The chunk provided as a parameter
     * |          | . . . . .|  . .  to this method (getPopulationRequirement).
     * |          | . . . . .|
     * |     #####|#####. . .|  x x  The chunk provided as a parameter
     * |     #####|#####. . .|  x x  to populate()
     * +----------+----------+
     * | x x #####|#####     |  ###  The area being populated by 'x'
     * | x x #####|#####     |  ###
     * | x x x x x|          |
     * | x x x x x|          |
     * +----------+----------+
     *
     * +----------+----------+  . .  The chunk provided as a parameter
     * |          | . . . . .|  . .  to this method (getPopulationRequirement).
     * |          | . . . . .|
     * |          | . . #####|  x x  The chunk provided as a parameter
     * |          | . . #####|  x x  to populate()
     * +----------+----------+
     * |          | x x #####|  ###  The area being populated by 'x'
     * |          | x x #####|  ###
     * |          | x x x x x|
     * |          | x x x x x|
     * +----------+----------+
     *
     * +----------+----------+  . .  The chunk provided as a parameter
     * | x x #####|#####. . .|  . .  to this method (getPopulationRequirement).
     * | x x #####|#####. . .|
     * | x x x x x| . . . . .|  x x  The chunk provided as a parameter
     * | x x x x x| . . . . .|  x x  to populate()
     * +----------+----------+
     * |          |          |  ###  The area being populated by 'x'
     * |          |          |  ###
     * |          |          |
     * |          |          |
     * +----------+----------+
     *
     * Shown: The area enclosed by the population requirement of the target cube
     *
     * </pre>
     * This method would return {@code {(-1,-1,-1), (0, 0, 0)}}, indicating that populate calls to all cubes in
     * that area write to this cube.<br> <br>
     *
     * Note: Large ranges are not recommended. If you need to generate a large structure like a nether fort, do not use
     * a populator.
     *
     * @param cube The target cube
     *
     * @return The bounding box of all cubes potentially writing to {@code cube}
     */
    Box getPopulationRequirement(Cube cube);

    /**
     * Called to reload structures that apply to {@code cube}. Mostly used to prepare calls to
     * {@link ICubeGenerator#getPossibleCreatures(EnumCreatureType, BlockPos))} <br>
     *
     * @param cube The cube being loaded
     *
     * @see ICubeGenerator#recreateStructures(IColumn) for the 2D-equivalent of this method
     */
    void recreateStructures(Cube cube);

    /**
     * Called to reload structures that apply to {@code column}. Mostly used to prepare calls to
     * {@link ICubeGenerator#getPossibleCreatures(EnumCreatureType, BlockPos))} <br>
     *
     * @param column The column being loaded
     *
     * @see ICubeGenerator#recreateStructures(Cube) for the 3D-equivalent of this method
     */
    void recreateStructures(IColumn column);

    /**
     * Retrieve a list of creature classes eligible for spawning at the specified location.
     *
     * @param type the creature type that we are interested in spawning
     * @param pos the position we want to spawn creatures at
     *
     * @return a list of creature classes that can spawn here. Example: Calling this method inside a nether fortress
     * returns EntityBlaze, EntityPigZombie, EntitySkeleton, and EntityMagmaCube
     */
    List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType type, BlockPos pos);

    /**
     * Gets the closest structure with name {@code name}. This is primarily used when an eye of ender is trying to find
     * a stronghold.
     *
     * @param name the name of the structure
     * @param pos find the structure closest to this position
     * @param findUnexplored true if should also find not yet generated structures
     *
     * @return the position of the structure, or <code>null</code> if none could be found
     */
    @Nullable BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored);
}
