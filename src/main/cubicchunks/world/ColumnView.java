/*
 *  This file is part of Cubic Chunks, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2014 Tall Worlds
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
package cubicchunks.world;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockPos;
import net.minecraft.world.LightType;
import cubicchunks.util.Coords;
import cubicchunks.util.RangeInt;

public class ColumnView extends Column {
	
	private Column column;
	private Map<Integer,Cube> cubes;
	
	public ColumnView(Column column) {
		super(column.getWorld(), column.chunkX, column.chunkZ);
		
		this.column = column;
		this.cubes = new TreeMap<Integer,Cube>();
	}
	
	@Override
	public LightIndex getLightIndex() {
		return this.column.getLightIndex();
	}
	
	@Override
	public Collection<Cube> getCubes() {
		return this.cubes.values();
	}
	
	public void addCubeToView(Cube cube) {
		this.cubes.put(cube.getY(), cube);
	}
	
	@Override
	public Cube getCube(int y) {
		return this.cubes.get(y);
	}
	
	public Cube getOrCreateCube(int y) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Iterable<Cube> getCubes(int minY, int maxY) {
		return ((TreeMap<Integer, Cube>) this.cubes).subMap(minY, true, maxY, true).values();
	}
	
	public void addCube(Cube cube) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<RangeInt> getCubeYRanges() {
		return getRanges(this.cubes.keySet());
	}
	
	@Override
	public boolean needsSaving(boolean alwaysTrue) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public IBlockState getBlockState(BlockPos pos) {
		
		// pass off to the cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			return cube.getBlockState(pos);
		}
		
		return Blocks.AIR.getDefaultState();
	}
	
	@Override
	public IBlockState setBlockState(BlockPos pos, IBlockState newBlockState) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int getBlockMetadata(BlockPos pos) {
		return this.column.getBlockMetadata(pos);
	}
	
	@Override
	public int getTopCubeY() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean getAreLevelsEmpty(int minBlockY, int maxBlockY) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean canSeeSky(BlockPos pos) {		
		return this.column.canSeeSky(pos);
	}
	
	@Override
	@Deprecated
	public int getHeightAtCoords(int localX, int localZ) {
		return this.column.getHeightAtCoords(localX, localZ);
	}
	
	@Override
	// getOpacity
	public int getBlockOpacityAt(BlockPos pos) {
		return this.column.getBlockOpacityAt(pos);
	}
	
	@Override
	public void addEntity(Entity entity) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeEntity(Entity entity) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeEntity(Entity entity, int cubeY) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public BlockEntity getBlockEntityAt(BlockPos pos, ChunkEntityCreationType creationType) {
		return this.column.getBlockEntityAt(pos, creationType);
	}
	
//	@Override
//	public void addTileEntity(Entity tileEntity) {
//		throw new UnsupportedOperationException();
//	}
	
	@Override
	// addTileEntity
	public void setBlockEntityAt(BlockPos pos, BlockEntity blockEntity) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeBlockEntityAt(BlockPos pos) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void onChunkLoad() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void onChunkUnload() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void readChunkIn(byte[] data, int segmentsToCopyBitFlags, boolean isFirstTime) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void tickChunk(boolean tryToTickFaster) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void generateSkylightMap() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public BlockPos getRainfallHeight(BlockPos pos) {
		return this.column.getRainfallHeight(pos);
	}
	
	@Override
	public int getBrightestLight(BlockPos pos, int skylightDampeningTerm) {
		return this.column.getBrightestLight(pos, skylightDampeningTerm);
	}
	
	@Override
	public int getLightAt(LightType lightType, BlockPos pos) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setLightAt(LightType lightType, BlockPos pos, int light) {
		throw new UnsupportedOperationException();
	}
}