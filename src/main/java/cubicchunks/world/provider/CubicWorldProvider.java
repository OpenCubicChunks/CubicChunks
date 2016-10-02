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
package cubicchunks.world.provider;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.provider.ICubicWorldProvider;
import cubicchunks.world.type.ICubicWorldType;
import cubicchunks.worldgen.generator.DummyChunkGenerator;
import cubicchunks.worldgen.generator.IColumnGenerator;
import cubicchunks.worldgen.generator.ICubeGenerator;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkGenerator;

/**
 * CubicChunks WorldProvider for Overworld.
 */
public abstract class CubicWorldProvider extends WorldProvider implements ICubicWorldProvider{
	@Override
	public int getHeight() {
		return ((ICubicWorld) this.worldObj).getMaxHeight();
	}

	@Override
	public int getActualHeight() {
		return hasNoSky ? 128 : getHeight();
	}

	/**
	 * Return Double.NaN to remove void fog and fix night vision potion below Y=0.
	 * <p>
	 * In EntityRenderer.updateFogColor entity Y position is multiplied by
	 * value returned by this method.
	 * <p>
	 * If this method returns any real number - then the void fog factor can be <= 0.
	 * But if this method returns NaN - the result is always NaN. And Minecraft enables void fog only of the value is < 1.
	 * And since any comparison with NaN returns false - void fog is effectively disabled.
	 */
	@Override
	public double getVoidFogYFactor(){
		return Double.NaN;
	}

	@Override
	@Deprecated
	public IChunkGenerator createChunkGenerator() {
		return new DummyChunkGenerator(this.worldObj);
	}

	@Override
	public ICubeGenerator createCubeGenerator() {
		// We need to assume that its an ICubicWorldType...
		// There is really nothing else we can do as a non-overworld porvider
		// that works with a vanilla world type would have overriden this method.
		return ((ICubicWorldType)this.worldObj.getWorldInfo().getTerrainType())
				.createCubeGenerator((ICubicWorldServer)this.worldObj);
	}

	@Override
	public IColumnGenerator createColumnGenerator() {
		// We need to assume that its an ICubicWorldType...
		// There is really nothing else we can do as a non-overworld porvider
		// that works with a vanilla world type would have overriden this method.
		return ((ICubicWorldType)this.worldObj.getWorldInfo().getTerrainType())
				.createColumnGenerator((ICubicWorldServer)this.worldObj);
	}

	@Override
	@Deprecated
	public boolean canDropChunk(int x, int z) {
		return true;
	}

	@Override
	public boolean canCoordinateBeSpawn(int x, int z) {
		//TODO: DONT USE WORLD.getGroundAboveSeaLevel()
		BlockPos blockpos = new BlockPos(x, 0, z);
		return this.worldObj.getBiome(blockpos).ignorePlayerSpawnSuitability() ? true
				: this.worldObj.getGroundAboveSeaLevel(blockpos).getBlock() == Blocks.GRASS;
	}

	@Override
	public BlockPos getRandomizedSpawnPoint() {
		//TODO: DONT USE World.getTopSolidOrLiquidBlock()
		return super.getRandomizedSpawnPoint();
	}
}
