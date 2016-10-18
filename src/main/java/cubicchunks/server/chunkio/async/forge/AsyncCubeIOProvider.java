/*
 * Minecraft Forge
 * Copyright (c) 2016.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package cubicchunks.server.chunkio.async.forge;

import cubicchunks.CubicChunks;
import cubicchunks.server.chunkio.CubeIO;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Async loading of cubes
 */
class AsyncCubeIOProvider extends AsyncIOProvider<Cube> {
	private final QueuedCube cubeInfo;
	private final CubeIO loader;

	private Column column;
	private Cube cube;

	AsyncCubeIOProvider(@Nonnull QueuedCube cube, @Nonnull Column column, @Nonnull CubeIO loader) {
		this.cubeInfo = cube;
		this.column = column;
		this.loader = loader;
	}

	@Override
	public synchronized void run() {
		try {
			cube = this.loader.loadCubeAndAddToColumn(column, this.cubeInfo.y);
		} catch (IOException e) {
			CubicChunks.LOGGER.error("Could not load cube in {} @ ({}, {}, {})", this.cubeInfo.world, this.cubeInfo.x, this.cubeInfo.y, this.cubeInfo.z, e);
		} finally {
			this.finished = true;
			this.notifyAll();
		}
	}

	// sync stuff
	@Override
	public void runSynchronousPart() {

		// TBD:
		// this.provider.cubeGenerator.recreateStructures(this.cube, this.cubeInfo.x, this.cubeInfo.z);

		this.runCallbacks();
	}

	@Override
	public Cube get() {
		return cube;
	}
}
