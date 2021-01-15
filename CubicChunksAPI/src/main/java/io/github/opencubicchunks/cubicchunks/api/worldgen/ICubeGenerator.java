/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.api.worldgen;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import io.github.opencubicchunks.cubicchunks.api.util.Box;
import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import mcp.MethodsReturnNonnullByDefault;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ICubeGenerator {

    Box RECOMMENDED_FULL_POPULATOR_REQUIREMENT = new Box(
            -1, -1, -1, // ignore jungle trees and other very tall structures and let them reach potentially unloaded cubes
            0, 0, 0
    );

    Box RECOMMENDED_GENERATE_POPULATOR_REQUIREMENT = new Box(
            1, 1, 1, // ignore jungle trees and other very tall structures and let them reach potentially unloaded cubes
            0, 0, 0
    );

    Box NO_REQUIREMENT = new Box(
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
     * @return A CubePrimer with the generated blocks
     * @deprecated generators are advised to implement {@link #generateCube(int, int, int, CubePrimer)}
     */
    @Deprecated
    CubePrimer generateCube(int cubeX, int cubeY, int cubeZ);

    /**
     * Generate a new cube
     *
     * @param cubeX the cube's X coordinate
     * @param cubeY the cube's Y coordinate
     * @param cubeZ the cube's Z coordinate
     * @param primer a CubePrimer which may be used for generating the new cube. Note that generators are allowed to return an
     *               arbitrary CubePrimer, this parameter is simply a hint to help reduce allocations. However, if a different
     *               primer is to be returned, the given primer's state must remain unmodified.
     *
     * @return A CubePrimer with the generated blocks
     */
    default CubePrimer generateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer) {
        // proxy to legacy generateCube method to retain API backwards-compatibility
        return this.generateCube(cubeX, cubeY, cubeZ);
    }

    /**
     * Generate column-global information such as biome data
     *
     * @param column the target column
     */
    void generateColumn(Chunk column);

    /**
     * Populate a cube with multi-block structures that can cross cube boundaries such as trees and ore veins.
     * Population should* be done with the restriction that it may not affect cubes whose call to
     * {@link ICubeGenerator#getFullPopulationRequirements(ICube)} would does include {@code cube}.
     *
     * Note: Unlike vanilla this method will NEVER cause recursive generation, thus the area that it populates is not as strict.
     * Generation should still be restricted as the player might see something generate in a chunk they have already been sent
     *
     * @param cube the cube to populate
     */
    void populate(ICube cube);

    default Optional<CubePrimer> tryGenerateCube(int cubeX, int cubeY, int cubeZ, CubePrimer primer, boolean forceGenerate) {
        return Optional.of(this.generateCube(cubeX, cubeY, cubeZ, primer));
    }

    default Optional<Chunk> tryGenerateColumn(World world, int columnX, int columnZ, ChunkPrimer primer, boolean forceGenerate) {
        Chunk column = new Chunk(world, columnX, columnZ);
        this.generateColumn(column);
        return Optional.of(column);
    }

    default boolean supportsConcurrentCubeGeneration() {
        return false;
    }

    default boolean supportsConcurrentColumnGeneration() {
        return false;
    }

    /**
     * Checks whether the generator is ready to generate a given cube.
     */
    default GeneratorReadyState pollAsyncCubeGenerator(int cubeX, int cubeY, int cubeZ) {
        return GeneratorReadyState.READY;
    }

    /**
     * Checks whether the generator is ready to generate a given column.
     */
    default GeneratorReadyState pollAsyncColumnGenerator(int chunkX, int chunkZ) {
        return GeneratorReadyState.READY;
    }

    /**
     * Checks whether the generator is ready to generate a given column.
     */
    default GeneratorReadyState pollAsyncCubePopulator(int cubeX, int cubeY, int cubeZ) {
        return GeneratorReadyState.READY;
    }

    /**
     * Get the bounding box defining a range of cubes whose population contributes to {@code cube} being fully
     * populated. The requested cubes will all be generated and populated when the cube that they affect needs to be fully populated.
     *
     * Consider the following example: A call to some implementation of
     * {@link ICubeGenerator#populate(ICube)} populates a 16x16x16 block area in a 2x2x2 area of cubes around the center.
     * <pre>
     *
     * Shown: Which cubes affect the current cube (marked with dots), and their corresponding areas of population.
     *
     * +----------+----------+  . .  The chunk provided as a parameter
     * |          | . . . . .|  . .  to this method (getFullPopulationRequirements).
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
     * |          | . . . . .|  . .  to this method (getFullPopulationRequirements).
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
     * | x x #####|#####. . .|  . .  to this method (getFullPopulationRequirements).
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
     * </pre>
     *
     * This method would return {@code {(-1,-1,-1), (0, 0, 0)}}, indicating that populate calls to all cubes in
     * that area write to this cube.<br> <br>
     *
     * Note: Large ranges are not recommended. If you need to generate a large structure like a nether fort, look at how MInecraft generates such
     * structures
     *
     * Also @{see #getPopulationPregenerationRequirements}
     *
     * @param cube The target cube
     *
     * @return The bounding box of all cubes potentially writing to {@code cube}
     */
    Box getFullPopulationRequirements(ICube cube);

    /**
     * Get the Box will all the cubes that populating this cube will affect this cube. These cubes will be generated before the cube is populated,
     * even if it's not full population.
     *
     * @param cube The target cube
     *
     * @return The bounding box of all cubes that the target cube may modify or use
     */
    Box getPopulationPregenerationRequirements(ICube cube);

    /**
     * Called to reload structures that apply to {@code cube}. Mostly used to prepare calls to
     * {@link ICubeGenerator#getPossibleCreatures(EnumCreatureType, BlockPos)} <br>
     *
     * @param cube The cube being loaded
     *
     * @see ICubeGenerator#recreateStructures(Chunk) for the 2D-equivalent of this method
     */
    void recreateStructures(ICube cube);

    /**
     * Called to reload structures that apply to {@code column}. Mostly used to prepare calls to
     * {@link ICubeGenerator#getPossibleCreatures(EnumCreatureType, BlockPos)} <br>
     *
     * @param column The column being loaded
     *
     * @see ICubeGenerator#recreateStructures(ICube) for the 3D-equivalent of this method
     */
    void recreateStructures(Chunk column);

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
     * @return the position of the structure, or {@code null} if none could be found
     */
    @Nullable BlockPos getClosestStructure(String name, BlockPos pos, boolean findUnexplored);

    enum GeneratorReadyState {
        /**
         * Indicates that the generator is ready to generate a given cube or column
         */
        READY,
        /**
         * Indicates that the generator is waiting for some resources to generate a given cube or column.
         * Generating may be possible in this state, but it could fail and take longer amount of time.
         */
        WAITING,
        /**
         * Generating a cube or column will most likely fail in this state.
         */
        FAIL
    }
}
