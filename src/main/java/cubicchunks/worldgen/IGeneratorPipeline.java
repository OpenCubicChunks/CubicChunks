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

package cubicchunks.worldgen;

import cubicchunks.util.CubeCoords;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.worldgen.dependency.DependentCubeManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IGeneratorPipeline {

	/**
	 * Returns the number of cubes that are queued for generation.
	 *
	 * @return The number of cubes that are queued for generation.
	 */
	int getQueuedCubeCount();

	/**
	 * Returns the GeneratorStageRegistry used by this IGeneratorPipeline. This registry dictates the stages every cube
	 * has to go through and the processors used in each given stage. Modifications on this registry are strongly
	 * discouraged as they are likely to disrupt the generation of currently queued cubes.
	 *
	 * @return The GeneratorStageRegistry used by this IGeneratorPipeline.
	 */
	@Nonnull
	GeneratorStageRegistry getGeneratorStageRegistry();

	/**
	 * Returns the DependentCubeManager used by this IGeneratorPipeline. The DependentCubeManager keeps track of all
	 * cubes requiring other cubes to be loaded for their generation. Removing cubes that have been registered by the
	 * IGeneratorPipeline from the DependentCubeCache will result in them not being generated further or may disrupt
	 * its processing.
	 *
	 * @return The DependentCubeManager used by this IGeneratorPipeline.
	 */
	@Nonnull
	DependentCubeManager getDependentCubeManager();

	/**
	 * Immediately queues the given cube for generation. All cubes required for processing the given cube must be
	 * loaded prior to passing it to this method. If a required cube is not available, undocumented behaviour might
	 * occur.
	 *
	 * @param cube The cube to be queued for generation.
	 */
	void resumeCube(@Nonnull Cube cube);

	/**
	 * Schedules the given cube for generation. The cube will be generated up to its set target stage. This
	 * IGeneratorPipeline will ensure that all cubes required for generating this cube are available when needed.
	 *
	 * @param cube The cube to be generated.
	 */
	void generateCube(@Nonnull Cube cube);

	/**
	 * Schedules the given cube for generation up to the given target stage.
	 * @see #generateCube(Cube)
	 *
	 * @param cube The cube to be generated.
	 */
	void generateCube(@Nonnull Cube cube, @Nonnull GeneratorStage targetStage);

	/**
	 * Schedules the cube at the given coordinates for generation. If the cube does already exist, updates the target
	 * stage if necessary. Otherwise a new cube object will be created and added to the proper column. If the cube did
	 * not yet exist or if it had not reached the target stage, it will be queued for generation.
	 * @see #generateCube(Cube)
	 *
	 * @param coords The coordinates of the cube to be generated.
	 * @param targetStage The target stage up until which the cube is supposed to be generated.
	 * @return The cube being generated or null if the cube did not already exist and creating it failed.
	 */
	@Nullable
	Cube generateCube(@Nonnull CubeCoords coords, @Nonnull GeneratorStage targetStage);

	/**
	 * Schedules the cube at the given coordinates for generation. The cube will be generated up to the final stage
	 * GeneratorStage.LIVE at which point it is considered to be final.
	 * @see #generateCube(CubeCoords, GeneratorStage)
	 * @see #generateCube(Cube)
	 *
	 * @param coords The coordinates of the cube to be generated.
	 * @return The cube being generated or null if the cube did not already exist and creating it failed.
	 */
	@Nullable
	Cube generateCube(@Nonnull CubeCoords coords);

	/**
	 * Creates a new column for the given coordinates.
	 *
	 * @param cubeX The x-coordinate of the column to be created.
	 * @param cubeZ The z-coordinate of the column to be created.
	 * @return A new instance of Column for the given coordinates.
	 */
	@Nullable
	Column generateColumn(int cubeX, int cubeZ);

	/**
	 * Removes the cube at the given coordinates from the generator queue.
	 *
	 * @param coords The coordinates of the cube to be removed from the generator queue.
	 */
	void removeCube(@Nonnull CubeCoords coords);

	/**
	 * Processes all currently queued cubes up to their respective target stage.
	 */
	void calculateAll();

	/**
	 * Processes queued cubes.
	 */
	void tick();
}
