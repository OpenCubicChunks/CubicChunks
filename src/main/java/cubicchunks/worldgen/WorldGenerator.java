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

import com.google.common.collect.ComparisonChain;
import cubicchunks.server.ServerCubeCache;
import cubicchunks.util.CubeCoords;
import cubicchunks.util.Progress;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.worldgen.dependency.DependentCube;
import cubicchunks.worldgen.dependency.DependentCubeManager;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cubicchunks.CubicChunks.LOGGER;

public class WorldGenerator implements IGeneratorPipeline {

	private static final int TICK_BUDGET = 40; //ms

	private final ICubicWorldServer world;

	private final ServerCubeCache cubeCache;

	private final ColumnGenerator columnGenerator;

	private final GeneratorStageRegistry generatorStageRegistry;

	private final DependentCubeManager dependentCubeManager;

	private final List<CubeCoords> toUpdate;

	private final Set<CubeCoords> toUpdateSet;

	private boolean needsSort;

	private int ticksSinceSorting;

	// Reporting

	private int[] stageProcessedRecent;

	private int[] stageProcessedTotal;

	private int processedRecent;

	private int processedTotal;

	private int skippedRecent;

	private int skippedTotal;

	private long durationTotal;

	private long durationRecent;


	public WorldGenerator(@Nonnull ICubicWorldServer world, @Nonnull GeneratorStageRegistry generatorStageRegistry) {

		this.world = world;
		this.cubeCache = world.getCubeCache();
		this.columnGenerator = new ColumnGenerator(this.world);
		this.generatorStageRegistry = generatorStageRegistry;
		this.dependentCubeManager = new DependentCubeManager(this.cubeCache.getDependencyManager());

		this.toUpdate = new ArrayList<>();
		this.toUpdateSet = new HashSet<>();

		this.needsSort = false;
		this.ticksSinceSorting = 0;

		// Note: At this point in time, the pipeline does not yet contain LIVE. Thus, the array sizes.
		this.stageProcessedRecent = new int[generatorStageRegistry.stageCount()];
		this.stageProcessedTotal = new int[generatorStageRegistry.stageCount()];
		this.processedRecent = 0;
		this.processedTotal = 0;
		this.skippedRecent = 0;
		this.skippedTotal = 0;
		this.durationRecent = 0L;
		this.durationTotal = 0L;
	}


	// ------------------------------------------- Interface: IGeneratorPipeline -------------------------------------------

	@Override
	public int getQueuedCubeCount() {
		return this.toUpdateSet.size();
	}

	@Nonnull
	public GeneratorStageRegistry getGeneratorStageRegistry() {
		return this.generatorStageRegistry;
	}

	@Override @Nonnull
	public DependentCubeManager getDependentCubeManager() {
		return this.dependentCubeManager;
	}

	@Override
	public void resumeCube(@Nonnull  Cube cube) {
		CubeCoords coords = cube.getCoords();
		if (toUpdateSet.add(coords)) {
			this.toUpdate.add(coords);
			this.needsSort = true;
		}
	}

	@Override
	public void generateCube(@Nonnull  Cube cube) {

		// If the cube has reached its target stage, don't do anything.
		if (cube.hasReachedTargetStage()) {
			return;
		}

		// If the cube has dependencies, register it at the dependency manager and let it handle the rest.
		CubeDependency cubeDependency = cube.getCurrentStage().getCubeDependency(cube);
		if (cubeDependency != null) {
			DependentCube dependentCube = new DependentCube(this, cube, cubeDependency);
			this.dependentCubeManager.register(dependentCube);
		}
		// Otherwise, resume generation.
		else {
			this.resumeCube(cube);
		}
	}

	@Override
	public void generateCube(@Nonnull Cube cube, @Nonnull GeneratorStage targetStage) {

		// Make sure the proper target stage is set.
		if (cube.getTargetStage().precedes(targetStage)) {
			cube.setTargetStage(targetStage);
		}

		this.generateCube(cube);
	}

	@Override @Nullable
	public Cube generateCube(@Nonnull CubeCoords coords, @Nonnull GeneratorStage targetStage) {

		// Get the cube's column. It must've been loaded in advance.
		Column column = this.cubeCache.getColumn(coords.getCubeX(), coords.getCubeZ());
		if (column == null) {
			return null;
		}

		// Create the cube object.
		Cube cube = column.getOrCreateCube(coords.getCubeY(), true);

		// Set the current stage.
		if (cube.getCurrentStage() == null) {
			cube.setCurrentStage(this.generatorStageRegistry.getFirstStage());
		}

		// Set the target stage.
		if (cube.getTargetStage() == null || cube.getTargetStage().precedes(targetStage)) {
			cube.setTargetStage(targetStage);
		}

		// Generate the cube if necessary (check performed in generateCube)
		this.generateCube(cube);

		return cube;
	}

	@Override @Nullable
	public Cube generateCube(@Nonnull CubeCoords coords) {
		return this.generateCube(coords, GeneratorStage.LIVE);
	}

	@Override @Nullable
	public Column generateColumn(int cubeX, int cubeZ) {
		return this.columnGenerator.generateColumn(cubeX, cubeZ);
	}

	@Override
	public void calculateAll() {

		long timeStart = System.currentTimeMillis();

		int processed = 0;
		int skipped = 0;
		int finished = 0;
		int[] cubesInStage = new int[this.generatorStageRegistry.stageCount()];

		Progress progress = new Progress(Integer.MAX_VALUE, 1000);

		while (!toUpdateSet.isEmpty()) {
			GeneratorStage stage = processNext();
			if (stage == null) {
				++skipped;
				continue;
			}

			// Reporting
			if (stage.isLastStage()) {
				++finished;
			} else {
				++cubesInStage[stage.getOrdinal()];
			}

			++processed;
			if (processed % 100 == 0) {
				this.updateProgress(progress, cubesInStage, finished, skipped);
			}
		}

		// Reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		this.durationTotal += timeDiff;
		this.durationRecent += timeDiff;
	}

	@Override
	public void tick() {

		// Sort cube stack if it needs to be sorted, but at most every 5 ticks.
		if (this.ticksSinceSorting >= 5 && this.needsSort) {
			this.sort();
			this.ticksSinceSorting = 0;
		}
		++this.ticksSinceSorting;

		long timeStart = System.currentTimeMillis();
		long timeEnd = timeStart + TICK_BUDGET;

		do {
			// If no cubes require updates, make sure the queue is empty and break.
			if (this.toUpdateSet.isEmpty()) {
				this.toUpdate.clear();
				break;
			}

			// Process the next cube.
			GeneratorStage stage = processNext();

		} while (System.currentTimeMillis() < timeEnd);

		// Reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		this.durationTotal += timeDiff;
		this.durationRecent += timeDiff;

		this.report();
		this.resetReport();
	}

	@Override
	public void removeCube(@Nonnull CubeCoords coords) {
		Cube cube = this.cubeCache.getCube(coords);
		if(cube != null) {
			//removing from array list is a bit expensive, remove from set and skip cubes that don't exist in the set
			this.toUpdateSet.remove(coords);
			this.dependentCubeManager.unregister(cube);
		}
	}

	public void report() {

		if (LOGGER.isDebugEnabled() && processedRecent > 0) {
			LOGGER.debug("Processed {} cubes, skipped {} in {} ms ({} in queue, {} dependents}", processedRecent, skippedRecent, durationRecent, this.toUpdateSet.size(), this.dependentCubeManager.getDependentCubeCount());
			for (GeneratorStage stage : this.generatorStageRegistry) {
				if (stage != GeneratorStage.LIVE) {
					String message = String.format("\t%15s: %3d processed", stage.getName(), stageProcessedRecent[stage.getOrdinal()]);
					LOGGER.debug(message);
				}
			}
		}

	}

	public void resetReport() {
		Arrays.fill(this.stageProcessedRecent, 0);
		this.processedRecent = 0;
		this.skippedRecent = 0;
	}


	// ---------------------------------------------------- Helper -----------------------------------------------------

	//TODO: Track progress properly
	private void updateProgress(@Nonnull Progress progress, int[] cubesInStage, int finished, int skipped) {
		int[] actualStages = new int[cubesInStage.length + 1];
		System.arraycopy(cubesInStage, 0, actualStages, 0, cubesInStage.length);
		actualStages[actualStages.length - 1] = finished + skipped;
		int totalKnownUnfinished = 0;
		//the amount of cubes that went through 0-th stage
		totalKnownUnfinished += cubesInStage[0];
		for (int n = 1; n < cubesInStage.length; n++) {
			//the amount if cubes that went through n-th stage but didn't go through (n-1)-th stage.
			//TODO: is it accurate enough?
			totalKnownUnfinished += Math.max(0, cubesInStage[n] - cubesInStage[n - 1]);
		}
		int unknown = toUpdateSet.size() + finished + skipped - totalKnownUnfinished;
		if (unknown < 0) {
			unknown = 0;
		}
		//if unknown, assume 0
		actualStages[0] += unknown;
		for (int i = 0; i < actualStages.length; i++) {
			int currentStage = (i == actualStages.length - 1) ?
					actualStages[i] :
					actualStages[i] - actualStages[i + 1];
			if (currentStage < 0) {
				currentStage = 0;
			}
			actualStages[i] = currentStage;
		}
		int total = 0;
		for (int num : actualStages) {
			total += num;
		}
		double totalProgress = 0;
		for (int i = 0; i < actualStages.length; i++) {
			double progressPart = actualStages[i]/(double) total;
			double weightedProgress = progressPart*i/(double) (actualStages.length - 1);
			totalProgress += weightedProgress;
		}
		assert totalProgress < 1.00001 && totalProgress >= 0;
		if (totalProgress > 1) {
			totalProgress = 1;
		}
		int progressInt = (int) (totalProgress*Integer.MAX_VALUE);
		progress.setProgress(progressInt);
	}

	@Nullable
	private GeneratorStage processNext() {

		CubeCoords next = this.next();
		Cube cube = this.cubeCache.getCube(next);
		if (cube == null) {

			// Reporting
			++skippedRecent;
			++skippedTotal;

			return null;
		}
		GeneratorStage previousStage = cube.getCurrentStage();
		previousStage.getProcessor().calculate(cube);

		//will be re-registered with new stage in generate()
		this.dependentCubeManager.unregister(cube);
		cube.setCurrentStage(previousStage.getNextStage());
		this.dependentCubeManager.updateDependents(cube);

		// Continue generating. generate(Cube cube) will check if the cube has reached its target stage.
		this.generateCube(cube);

		// Reporting
		++processedRecent;
		++stageProcessedRecent[previousStage.getOrdinal()];
		++processedTotal;
		++stageProcessedTotal[previousStage.getOrdinal()];

		return previousStage;
	}

	@Nonnull
	private CubeCoords next() {
		//throws exception when there is no next cube
		while(true) {
			CubeCoords coords = this.toUpdate.remove(toUpdate.size() - 1);
			if(this.toUpdateSet.remove(coords)) {
				return coords;
			}
		}
	}

	private void sort() {
		if(this.toUpdate.size() != this.toUpdateSet.size()) {
			this.toUpdate.clear();
			this.toUpdate.addAll(toUpdateSet);
		}
		//TODO: make GeneratorStageRegistry sort() faster
		Collection<EntityPlayer> players = world.getPlayerEntities();

		//TODO: is it correct order?
		Collections.sort(this.toUpdate, (coords1, coords2) ->
				ComparisonChain.start().compare(
						getClosestPlayerDistance(coords2, players),
						getClosestPlayerDistance(coords1, players)
				).result());
	}

	private int getClosestPlayerDistance(@Nonnull CubeCoords coords, @Nonnull Collection<EntityPlayer> players) {
		int min = Integer.MAX_VALUE;
		for (EntityPlayer player : players) {
			int dist = CubeCoords.fromEntity(player).distSquared(coords);
			if (dist < min) {
				min = dist;
			}
		}
		return min;
	}

}
