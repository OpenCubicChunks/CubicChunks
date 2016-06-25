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
import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.worldgen.dependency.DependentCube;
import cubicchunks.worldgen.dependency.DependentCubeManager;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cubicchunks.CubicChunks.LOGGER;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class GeneratorPipeline {

	private static final int TickBudget = 40; // ms. There are only 50 ms per tick

	private final ICubicWorldServer world;
	private ServerCubeCache cubeProvider;

	private List<GeneratorStage> stages;
	private Map<String, GeneratorStage> stageMap;

	private DependentCubeManager dependentCubeManager;

	private ArrayList<CubeCoords> toUpdate = new ArrayList<>();
	private Set<CubeCoords> toUpdateSet = new HashSet<>();
	private boolean needsSort = false;
	private int tick = 0;


	public GeneratorPipeline(ICubicWorldServer world) {
		this.world = world;
		this.cubeProvider = world.getCubeCache();
		this.stages = new ArrayList<>();
		this.stageMap = new HashMap<>();
		this.stageMap.put(GeneratorStage.LIVE.getName(), GeneratorStage.LIVE);
		this.dependentCubeManager = new DependentCubeManager(cubeProvider.getDependencyManager());
	}

	public void addStage(GeneratorStage stage, CubeProcessor processor) {
		stage.setOrdinal(this.stages.size());
		stage.setProcessor(processor);
		this.stages.add(stage);
		this.stageMap.put(stage.getName(), stage);
	}

	public void checkStages() {
		for (GeneratorStage stage : this.stages) {
			if (!stage.isLastStage()) {
				if (stage.getProcessor() == null) {
					throw new Error("Generator pipeline configured incorrectly! Stage " + stage.getName() +
							" is null! Fix your WorldServerContext constructor!");
				}
			}
		}
	}


	public int getNumCubes() {
		return this.toUpdateSet.size();
	}

	public DependentCubeManager getDependentCubeManager() {
		return this.dependentCubeManager;
	}

	public void resume(Cube cube) {
		CubeCoords coords = cube.getCoords();
		if (toUpdateSet.add(coords)) {
			this.toUpdate.add(coords);
			this.needsSort = true;
		}
	}

	public void generate(Cube cube) {
		// If the cube has reached its target stage, don't do anything.
		if (cube.hasReachedTargetStage()) {
			return;
		}
		// If the cube has dependencies, register it at the dependency manager and let it handle the rest.
		CubeDependency cubeDependency = cube.getCurrentStage().getCubeDependency(cube);
		if (cubeDependency != null) {
			DependentCube dependentCube = new DependentCube(this, cube, cubeDependency);
			this.dependentCubeManager.register(dependentCube);
			// Otherwise, start processing.
		} else {
			resume(cube);
		}
	}

	public void generate(Cube cube, GeneratorStage targetStage) {
		// Make sure the proper target stage is set.
		if (cube.getTargetStage().precedes(targetStage)) {
			cube.setTargetStage(targetStage);
		}
		this.generate(cube);
	}

	public int tick() {
		//NOTE: there is no need to check if any player has moved
		//If a player has moved, new cubes are generated
		//and needsSort is set to true when new cubes are added
		if (tick%5 == 0 && needsSort) {
			sort();
		}
		tick++;

		final long timeStart = System.currentTimeMillis();
		final long timeEnd = timeStart + TickBudget;

		int[] stageProcessed = new int[this.stages.size()];
		int skipped = 0;
		int totalProcessed = 0;

		do {
			if (this.toUpdateSet.isEmpty()) {
				this.toUpdate.clear();
				break;
			}
			GeneratorStage stage = processNext();
			if (stage == null) {
				skipped++;
			} else {
				stageProcessed[stage.getOrdinal()]++;
				totalProcessed++;
			}
		} while (System.currentTimeMillis() < timeEnd);
		// reporting
		long timeDiff = System.currentTimeMillis() - timeStart;
		if (LOGGER.isDebugEnabled() && totalProcessed > 0) {
			LOGGER.debug("Processed {} cubes, skipped {} cubes in {} ms ({} in queue, {} dependents).", totalProcessed, skipped, timeDiff, this.toUpdateSet
					.size(), this.dependentCubeManager.getDependentCubeCount());
			for (GeneratorStage stage : this.stages) {
				String message = String.format("\t%15s: %3d processed", stage.getName(), stageProcessed[stage.getOrdinal()]);
				LOGGER.debug(message);
			}
		}

		return totalProcessed + skipped;
	}

	/**
	 * Processes a single cube.
	 * <p>
	 * Returns the generator stage it was in, or null if it was skipped
	 */
	@Nullable private GeneratorStage processNext() {
		CubeCoords next = next();
		Cube cube = this.cubeProvider.getCube(next);
		if (cube == null) {
			return null;
		}
		GeneratorStage previousStage = cube.getCurrentStage();
		previousStage.getProcessor().calculate(cube);

		//advance to next stage
		boolean isLastStage = previousStage.getOrdinal() + 1 >= this.stages.size();
		GeneratorStage nextStage = isLastStage ?
				GeneratorStage.LIVE :
				this.stages.get(previousStage.getOrdinal() + 1);

		//will be re-registered with new stage in generate()
		this.dependentCubeManager.unregister(cube);
		cube.setCurrentStage(nextStage);
		this.dependentCubeManager.updateDependents(cube);
		if (!isLastStage && nextStage.precedes(cube.getTargetStage())) {
			generate(cube);
		}
		return previousStage;
	}

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
			toUpdate.clear();
			toUpdate.addAll(toUpdateSet);
		}
		//TODO: make GeneratorPipeline sort() faster
		Collection<EntityPlayer> players = world.getPlayerEntities();

		//TODO: is it correct order?
		Collections.sort(this.toUpdate, (coords1, coords2) ->
				ComparisonChain.start().compare(
						getClosestPlayerDistance(coords2, players),
						getClosestPlayerDistance(coords1, players)
				).result());
	}

	private int getClosestPlayerDistance(CubeCoords coords, Collection<EntityPlayer> players) {
		int min = Integer.MAX_VALUE;
		for (EntityPlayer player : players) {
			int dist = CubeCoords.fromEntity(player).distSquared(coords);
			if (dist < min) {
				min = dist;
			}
		}
		return min;
	}

	public void generateAll() {
		int skipped = 0;
		int finished = 0;
		//number of cubes that have reached n-th stage
		int[] cubesInStage = new int[this.stages.size()];
		Progress progress = new Progress(Integer.MAX_VALUE, 1000);
		int numUpdated = 0;
		while (!toUpdateSet.isEmpty()) {
			numUpdated++;
			GeneratorStage stage = processNext();
			if (stage == null) {
				skipped++;
				continue;
			}
			int currentStageOrdinal = stage.getOrdinal() + 1;
			if (currentStageOrdinal >= cubesInStage.length) {
				finished++;
			} else {
				cubesInStage[currentStageOrdinal]++;
			}
			if (numUpdated%50 == 0) {
				updateProgress(progress, cubesInStage, finished, skipped);
			}
		}
	}

	//TODO: Track progress properly
	private void updateProgress(Progress progress, int[] cubesInStage, int finished, int skipped) {
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

	@Nullable public GeneratorStage getStage(String name) {
		return this.stageMap.get(name);
	}

	public GeneratorStage getFirstStage() {
		return this.stages.get(0);
	}

	public void remove(CubeCoords coords) {
		Cube cube = this.cubeProvider.getCube(coords);
		if(cube != null) {
			//removing from array list is a bit expensive, remove from set and skip cubes that don't exist in the set
			this.toUpdateSet.remove(coords);
			this.dependentCubeManager.unregister(cube);
		}
	}
}
