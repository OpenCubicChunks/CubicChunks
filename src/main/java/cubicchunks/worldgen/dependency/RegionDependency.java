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

package cubicchunks.worldgen.dependency;

import cubicchunks.util.CubeCoords;
import cubicchunks.world.cube.Cube;
import cubicchunks.world.dependency.CubeDependency;
import cubicchunks.world.dependency.DependencyManager;
import cubicchunks.world.dependency.Requirement;
import cubicchunks.worldgen.GeneratorStage;
import net.minecraft.util.math.Vec3i;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Specifies the dependency of a Cube on other cubes in its vicinity.
 *
 * @see CubeDependency
 * @see DependencyManager
 */
public class RegionDependency implements CubeDependency {

	/**
	 * The minimum GeneratorStage all Cubes included in this region must be at for this CubeDependency to be satisfied.
	 */
	private GeneratorStage targetStage;

	/*
	 * The corners of the region.
	 */
	private int xLow;
	private int xHigh;
	private int yLow;
	private int yHigh;
	private int zLow;
	private int zHigh;

	/**
	 * Creates a new instance of RegionDependency. All Cubes within the given radius must be at the given GeneratorStage
	 * for this CubeDependency to be satisifed.
	 *
	 * @param targetStage The minimum GeneratorStage all Cubes within the given radius must be at for this
	 * CubeDependency to be satisfied.
	 * @param radius The radius of the cuboid including all required Cubes.
	 */
	public RegionDependency(@Nonnull GeneratorStage targetStage, int radius) {

		this.targetStage = targetStage;

		this.xLow = -radius;
		this.xHigh = radius;
		this.yLow = -radius;
		this.yHigh = radius;
		this.zLow = -radius;
		this.zHigh = radius;
	}

	/**
	 * Creates a new instance of RegionDependency. All Cubes inside of the cuboid specified by relA and relB must be at
	 * the given GeneratorStage for this CubeDependency to be satisfied. The relative orientation of relA and relB to
	 * each is irrelevant.
	 *
	 * @param targetStage The minimum GeneratorStage all Cubes within the specified region must be at for this
	 * CubeDependency to be satisfied.
	 * @param relA First corner of the cuboid including all required Cubes.
	 * @param relB Second corner of the cuboid including all required Cubes.
	 */
	public RegionDependency(@Nonnull GeneratorStage targetStage, @Nonnull Vec3i relA, @Nonnull Vec3i relB) {

		this.targetStage = targetStage;

		this.xLow = Math.min(relA.getX(), relB.getX());
		this.xHigh = Math.max(relA.getX(), relB.getX());
		this.yLow = Math.min(relA.getY(), relB.getY());
		this.yHigh = Math.max(relA.getY(), relB.getY());
		this.zLow = Math.min(relA.getZ(), relB.getZ());
		this.zHigh = Math.max(relA.getZ(), relB.getZ());
	}

	/*
	 * Interface: CubeDependency
	 */

	public Collection<Requirement> getRequirements(Cube cube) {

		List<Requirement> requirements = new ArrayList<>((xHigh - xLow + 1)*(yHigh - yLow + 1)*(zHigh - zLow + 1));

		int cubeX = cube.getX();
		int cubeY = cube.getY();
		int cubeZ = cube.getZ();

		for (int x = this.xLow; x <= this.xHigh; ++x) {
			for (int y = this.yLow; y <= this.yHigh; ++y) {
				for (int z = this.zLow; z <= this.zHigh; ++z) {
					if (x != 0 || y != 0 || z != 0) {
						CubeCoords coords = new CubeCoords(cubeX + x, cubeY + y, cubeZ + z);
						requirements.add(new Requirement(coords, targetStage));
					}
				}
			}
		}

		return requirements;
	}

	public boolean isSatisfied(DependencyManager manager, DependentCube dependentCube, Cube requiredCube) {
		return requiredCube.hasReachedStage(this.targetStage);
	}

}
