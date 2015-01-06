/*******************************************************************************
 * Copyright (c) 2014 Bartosz Skrzypczak.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Bartosz Skrzypczak - initial implementation and adaptation from vanilla.
 ******************************************************************************/
package cubicchunks.generator.features;

import java.util.Random;

import cubicchunks.world.Cube;
import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

public abstract class CubicMapGenBase
{
	/** The number of Chunks to gen-check in any given direction. */
	protected int m_range = 8;

	/** The RNG used by the MapGen classes. */
	protected Random m_rand = new Random();

	/** This world object. */
	protected World m_world;

	public void generate( World world, Cube cube )
	{
		int xOrigin = cube.getX();
		int yOrigin = cube.getY();
		int zOrigin = cube.getZ();
		
		int radius = this.m_range;
		this.m_world = world;
		this.m_rand.setSeed( world.getSeed() );
		long randX = this.m_rand.nextLong();
		long randY = this.m_rand.nextLong();
		long randZ = this.m_rand.nextLong();

		for ( int x = xOrigin - radius; x <= xOrigin + radius; ++x )
		{
			for ( int y = yOrigin - radius; y <= yOrigin + radius; ++y )
			{
				for ( int z = zOrigin - radius; z <= zOrigin + radius; ++z )
				{
					long randX_mul = ( long ) x * randX;
					long randY_mul = ( long ) y * randY;
					long randZ_mul = ( long ) z * randZ;
					this.m_rand.setSeed( randX_mul ^ randY_mul ^ randZ_mul ^ world.getSeed() );
					this.generate( world, cube, x, y, z, xOrigin, yOrigin, zOrigin );
				}
			}

		}
	}

	protected abstract void generate( World world, Cube cube, int x, int y, int z, int xOrig, int yOrig, int zOrig );
}
