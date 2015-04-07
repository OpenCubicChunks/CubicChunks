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
package cubicchunks.world.column;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.packet.clientbound.PacketChunkData;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Facing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ChunkSection;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;

import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.AddressTools;
import cubicchunks.util.Bits;
import cubicchunks.util.Coords;
import cubicchunks.util.RangeInt;
import cubicchunks.world.ChunkSectionHelper;
import cubicchunks.world.EntityContainer;
import cubicchunks.world.ICubeCache;
import cubicchunks.world.LightIndex;
import cubicchunks.world.WorldContext;
import cubicchunks.world.cube.Cube;

public class Column extends Chunk {
	
	public static final int LOAD_CUBES = 1;
	public static final int UNLOAD_CUBES = 2;
	
	private static final Logger log = LogManager.getLogger();
	
	private TreeMap<Integer,Cube> cubes;
	private LightIndex lightIndex;
	private int roundRobinLightUpdatePointer;
	private List<Cube> roundRobinCubes;
	private EntityContainer entities;
	
	public Column(World world, int x, int z) {
		
		// NOTE: this constructor is called by the chunk loader
		super(world, x, z);
		
		init();
	}
	
	public Column(World world, int cubeX, int cubeZ, Biome[] biomes) {
		
		// NOTE: this constructor is called by the column generator
		this(world, cubeX, cubeZ);
		
		init();
		
		// save the biome data
		for (int i = 0; i < biomes.length; i++) {
			biomeMap[i] = (byte)biomes[i].biomeID;
		}
		
		this.isModified = true;
	}
	
	private void init() {
		
		this.cubes = new TreeMap<Integer,Cube>();
		this.lightIndex = new LightIndex(this.world.getSeaLevel());
		this.roundRobinLightUpdatePointer = 0;
		this.roundRobinCubes = new ArrayList<Cube>();
		this.entities = new EntityContainer();
		
		// make sure no one's using data structures that have been replaced
		// also saves memory
		/* TODO: setting these vars to null would save memory, but they're final. =(
		 * also... make sure we're actually not using them
		this.chunkSections = null;
		this.biomeMap = null;
		this.heightMap = null;
		this.skylightUpdateMap = null;
		*/
		
		Arrays.fill(this.biomeMap, (byte)-1);
	}
	
	public long getAddress() {
		return AddressTools.getAddress(this.chunkX, this.chunkZ);
	}
	
	public int getX() {
		return this.chunkX;
	}
	
	public int getZ() {
		return this.chunkZ;
	}
	
	public EntityContainer getEntityContainer() {
		return this.entities;
	}
	
	public LightIndex getLightIndex() {
		return this.lightIndex;
	}
	
	@Override
	public void generateHeightMap() {
		// override this so no height map is generated
	}
	
	public Collection<Cube> getCubes() {
		return Collections.unmodifiableCollection(this.cubes.values());
	}
	
	public boolean hasCubes() {
		return !this.cubes.isEmpty();
	}
	
	public Cube getCube(int y) {
		return this.cubes.get(y);
	}
	
	public Cube getOrCreateCube(int cubeY, boolean isModified) {
		Cube cube = this.cubes.get(cubeY);
		if (cube == null) {
			cube = new Cube(this.world, this, this.chunkX, cubeY, this.chunkZ, isModified);
			this.cubes.put(cubeY, cube);
		}
		return cube;
	}
	
	public Iterable<Cube> getCubes(int minY, int maxY) {
		return this.cubes.subMap(minY, true, maxY, true).values();
	}
	
	public Cube removeCube(int cubeY) {
		return this.cubes.remove(cubeY);
	}
	
	public List<RangeInt> getCubeYRanges() {
		return getRanges(this.cubes.keySet());
	}
	
	@Override
	public boolean needsSaving(boolean alwaysTrue) {
		return this.entities.needsSaving(this.world.getGameTime()) || this.isModified;
	}
	
	public void markSaved() {
		this.entities.markSaved(this.world.getGameTime());
		this.isModified = false;
	}
	
	@Override
	public Block getBlockAt(final int x, final int y, final int z) {
		
		//compact to BlockPos
		BlockPos pos = new BlockPos(x, y, z);
		
		return getBlockAt(pos);
	}
	
	@Override
	public Block getBlockAt(final BlockPos pos) {
		// pass to cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			return cube.getBlockAt(pos);
		}
		
		return Blocks.AIR;
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
		
		// is there a chunk for this block?
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube == null) {
			return null;
		}
		
		// did anything change?
		IBlockState oldBlockState = cube.setBlockState(pos, newBlockState);
		if (oldBlockState == null) {
			// nothing changed
			return null;
		}
		
		Block oldBlock = oldBlockState.getBlock();
		Block newBlock = newBlockState.getBlock();
		
		// update rain map
		// NOTE: rainfallMap[xzCoord] is he lowest block that will contain rain
		// so rainfallMap[xzCoord] - 1 is the block that is being rained on
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		int xzCoord = z << 4 | x;
		if (pos.getY() >= this.rainfallMap[xzCoord] - 1) {
			// invalidate the rain height map value
			this.rainfallMap[xzCoord] = -999;
		}
		
		int newOpacity = newBlock.getOpacity();
		int oldOpacity = oldBlock.getOpacity();
		
		// did the top non-transparent block change?
		Integer oldSkylightY = getSkylightBlockY(x, z);
		getLightIndex().setOpacity(x, pos.getY(), z, newOpacity);
		Integer newSkylightY = getSkylightBlockY(x, z);
		if (oldSkylightY != null && newSkylightY != null && !oldSkylightY.equals(newSkylightY)) {
			
			// sort the y-values
			int minBlockY = oldSkylightY;
			int maxBlockY = newSkylightY;
			if (minBlockY > maxBlockY) {
				minBlockY = newSkylightY;
				maxBlockY = oldSkylightY;
			}
			assert (minBlockY < maxBlockY) : "Values not sorted! " + minBlockY + ", " + maxBlockY;
			
			// update light and signal render update
			WorldContext.get(this.world).getLightingManager().computeSkyLightUpdate(this, x, z, minBlockY, maxBlockY);
			this.world.markBlockRangeForRenderUpdate(pos.getX(), minBlockY, pos.getZ(), pos.getX(), maxBlockY, pos.getZ());
		}
		
		// if opacity changed and ( opacity decreased or block now has any light )
		int skyLight = getLightAt(LightType.SKY, pos);
		int blockLight = getLightAt(LightType.BLOCK, pos);
		if (newOpacity != oldOpacity && (newOpacity < oldOpacity || skyLight > 0 || blockLight > 0)) {
			WorldContext.get(this.world).getLightingManager().queueSkyLightOcclusionCalculation(pos.getX(), pos.getZ());
		}
		
		// update lighting index
		getLightIndex().setOpacity(x, pos.getY(), z, newBlock.getOpacity());
		
		this.isModified = true;
		
		// NOTE: after this method, the World calls updateLights on the source block which changes light values again
		
		return oldBlockState;
	}
	
	@Override
	public int getBlockMetadata(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		return getBlockMetadata(x, pos.getY(), z);
	}
	
	@Override
	public int getBlockMetadata(int localX, int blockY, int localZ) {
		
		// pass off to the cube
		int cubeY = Coords.blockToCube(blockY);
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			int localY = Coords.blockToLocal(blockY);
			IBlockState blockState = cube.getBlockState(localX, localY, localZ);
			if (blockState != null) {
				return blockState.getBlock().getMetadataForBlockState(blockState);
			}
		}
		return 0;
	}
	
	public int getTopCubeY() {
		return this.cubes.lastKey();
	}
	
	public int getBottomCubeY() {
		return this.cubes.firstKey();
	}
	
	public Integer getTopFilledCubeY() {
		Integer blockY = getLightIndex().getTopNonTransparentBlockY();
		if (blockY == null) {
			return null;
		}
		return Coords.blockToCube(blockY);
	}
	
	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it, but we need to be hacky about it
	public int getBlockStoreY() {
		Integer cubeY = getTopFilledCubeY();
		if (cubeY != null) {
			return Coords.cubeToMinBlock(cubeY);
		} else {
			// PANIC!
			// this column doesn't have any blocks in it that aren't air!
			// but we can't return null here because vanilla code expects there to be a surface down there somewhere
			// we don't actually know where the surface is yet, because maybe it hasn't been generated
			// but we do know that the surface has to be at least at sea level,
			// so let's go with that for now and hope for the best
			return this.world.getSeaLevel();
		}
	}
	
	@Override
	public boolean getAreLevelsEmpty(int minBlockY, int maxBlockY) {
		int minCubeY = Coords.blockToCube(minBlockY);
		int maxCubeY = Coords.blockToCube(maxBlockY);
		for (int cubeY = minCubeY; cubeY <= maxCubeY; cubeY++) {
			Cube cube = this.cubes.get(cubeY);
			if (cube != null && cube.hasBlocks()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean canSeeSky(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		Integer skylightBlockY = getSkylightBlockY(x, z);
		if (skylightBlockY == null) {
			return true;
		}
		return pos.getY() >= skylightBlockY;
	}
	
	public Integer getSkylightBlockY(int localX, int localZ) {
		// NOTE: a "skylight" block is the transparent block that is directly one block above the top non-transparent block
		Integer topBlockY = getLightIndex().getTopNonTransparentBlockY(localX, localZ);
		if (topBlockY != null) {
			return topBlockY + 1;
		}
		return null;
	}
	
	@Override
	@Deprecated
	// don't use this! It's only here because vanilla needs it, but we need to be hacky about it
	public int getHeightAtCoords(int localX, int localZ) {
		// NOTE: the "height value" here is the height of the transparent block on top of the highest non-transparent block
		
		Integer skylightBlockY = getSkylightBlockY(localX, localZ);
		if (skylightBlockY == null) {
			// PANIC!
			// this column doesn't have any blocks in it that aren't air!
			// but we can't return null here because vanilla code expects there to be a surface down there somewhere
			// we don't actually know where the surface is yet, because maybe it hasn't been generated
			// but we do know that the surface has to be at least at sea level,
			// so let's go with that for now and hope for the best
			skylightBlockY = this.world.getSeaLevel() + 1;
		}
		return skylightBlockY;
	}
	
	@Override
	public int getBlockOpacityAt(BlockPos pos) {
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		return getLightIndex().getOpacity(x, pos.getY(), z);
	}
	
	public Iterable<Entity> entities() {
		return this.entities.entities();
	}
	
	@Override
	public void addEntity(Entity entity) {
		int cubeY = Coords.getCubeYForEntity(entity);
		
		// pass off to the cube
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			cube.addEntity(entity);
		} else {
			// entities don't have to be in cubes, just add it directly to the column
			entity.addedToChunk = true;
			entity.chunkX = this.chunkX;
			entity.chunkY = cubeY;
			entity.chunkZ = this.chunkZ;
			
			this.entities.add(entity);
			this.isModified = true;
		}
	}
	
	@Override
	public void removeEntity(Entity entity) {
		removeEntity(entity, entity.chunkY);
	}
	
	@Override
	public void removeEntity(Entity entity, int cubeY) {
		
		if (!entity.addedToChunk) {
			return;
		}
		
		// pass off to the cube
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			cube.removeEntity(entity);
		} else if (this.entities.remove(entity)) {
			entity.addedToChunk = false;
			this.isModified = true;
		} else {
			log.warn("{} Tried to remove entity {} from column ({},{}), but it was not there. Entity thinks it's in cube ({},{},{})",
				this.world.isClient ? "CLIENT" : "SERVER",
				entity.getClass().getName(),
				this.chunkX, this.chunkZ,
				entity.chunkX, entity.chunkY, entity.chunkZ
			);
		}
	}
	
	@Override
    public void findEntitiesExcept(Entity excludedEntity, AxisAlignedBB queryBox, List<Entity> out, Predicate<? super Entity> predicate) {
		
		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor(queryBox.minY - 2));
		int maxCubeY = Coords.blockToCube(MathHelper.floor(queryBox.maxY + 2));
		for (Cube cube : getCubes(minCubeY, maxCubeY)) {
			cube.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
		}
		
		// check the column too
		this.entities.findEntitiesExcept(excludedEntity, queryBox, out, predicate);
    }
    
	@Override
    public <T extends Entity> void findEntities(Class<? extends T> entityType, AxisAlignedBB queryBox, List<T> out, Predicate<? super T> predicate) {
		
		// get a y-range that 2 blocks wider than the box for safety
		int minCubeY = Coords.blockToCube(MathHelper.floor(queryBox.minY - 2));
		int maxCubeY = Coords.blockToCube(MathHelper.floor(queryBox.maxY + 2));
		for (Cube cube : getCubes(minCubeY, maxCubeY)) {
			cube.findEntities(entityType, queryBox, out, predicate);
		}
		
		// check the column too
		this.entities.findEntities(entityType, queryBox, out, predicate);
    }
	
	@Override
	public BlockEntity getBlockEntityAt(BlockPos pos, ChunkEntityCreationType creationType) {
		
		// pass off to the cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			return cube.getBlockEntity(pos, creationType);
		}
		return null;
	}
	
	@Override
	public void setBlockEntityAt(BlockPos pos, BlockEntity blockEntity) {
		
		// pass off to the cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			cube.addBlockEntity(pos, blockEntity);
		} else {
			log.warn("No cube at ({},{},{}) to add tile entity (block {},{},{})!",
				this.chunkX, cubeY, this.chunkZ,
				pos.getX(), pos.getY(), pos.getZ()
			);
		}
	}
	
	@Override
	public void removeBlockEntityAt(BlockPos pos) {
		
		// pass off to the cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			cube.removeBlockEntity(pos);
		}
	}
	
	@Override
	public void onChunkLoad() {
		this.chunkLoaded = true;
	}
	
	@Override
	public void onChunkUnload() {
		this.chunkLoaded = false;
	}
	
	public PacketChunkData.EncodedChunk encode(boolean isFirstTime, boolean hasSky, int sectionFlags)
	throws IOException {
		
		if (!(this instanceof ColumnView)) {
			throw new Error("we should never be encoding whole columns!");
		}
		
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		
		// NOTE: there's no need to do compression here. This output is compressed later
		
		// how many cubes are we sending?
		int numCubes = getCubes().size();
		out.writeShort(numCubes);
		
		// send the actual cube data
		for (Cube cube : getCubes()) {
			
			// signal we're sending this cube
			out.writeInt(cube.getY());
			
			out.writeBoolean(cube.isEmpty());
			if (!cube.isEmpty()) {
				ChunkSection storage = cube.getStorage();
				
				// 1. block IDs, low bits
				out.write(ChunkSectionHelper.getBlockLSBArray(storage));
				
				// 2. block IDs, high bits
				NibbleArray blockIdMsbs = ChunkSectionHelper.getBlockMSBArray(storage);
				if (blockIdMsbs != null) {
					out.writeByte(1);
					out.write(blockIdMsbs.get());
				} else {
					// signal we're not sending this data
					out.writeByte(0);
				}
				
				// 3. metadata
				out.write(ChunkSectionHelper.getBlockMetaArray(storage).get());
				
				// 4. block light
				out.write(storage.getBlockLightArray().get());
				
				if (hasSky) {
					// 5. sky light
					out.write(storage.getSkyLightArray().get());
				}
			}
		}
		
		if (isFirstTime) {
			// 6. biomes
			out.write(getBiomeMap());
		}
		
		// 7. light index
		getLightIndex().writeData(out);
		
		out.close();
		
		PacketChunkData.EncodedChunk encodedChunk = new PacketChunkData.EncodedChunk();
		encodedChunk.data = buf.toByteArray();
		encodedChunk.sectionFlags = LOAD_CUBES;
		return encodedChunk;
	}
	
	public PacketChunkData.EncodedChunk encodeUnload()
	throws IOException {
		
		if (!(this instanceof ColumnView)) {
			throw new Error("we should never be encoding whole columns!");
		}
		
		// encode the number of cubes, and the y values
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(buf);
		int numCubes = getCubes().size();
		out.writeShort(numCubes);
		for (Cube cube : getCubes()) {
			out.writeInt(cube.getY());
		}
		out.close();
		
		PacketChunkData.EncodedChunk encodedChunk = new PacketChunkData.EncodedChunk();
		encodedChunk.data = buf.toByteArray();
		encodedChunk.sectionFlags = UNLOAD_CUBES;
		return encodedChunk;
	}
	
	@Override
	public void readChunkIn(byte[] data, int sectionFlags, boolean isFirstTime) {
		
		// NOTE: this is called on the client when it receives chunk data from the server
		
		if (sectionFlags == LOAD_CUBES) {
			loadCubes(data, isFirstTime);
		} else if (sectionFlags == UNLOAD_CUBES) {
			unloadCubes(data);
		}
	}
	
	private void loadCubes(byte[] data, boolean isFirstTime) {
		
		ByteArrayInputStream buf = new ByteArrayInputStream(data);
		DataInputStream in = new DataInputStream(buf);
		
		try {
			// how many cubes are we reading?
			int numCubes = in.readUnsignedShort();
			for (int i = 0; i < numCubes; i++) {
				int cubeY = in.readInt();
				Cube cube = getOrCreateCube(cubeY, false);
				
				// if the cube came from the server, it must be live
				cube.setGeneratorStage(GeneratorStage.getLastStage());
				
				// is the cube empty?
				boolean isEmpty = in.readBoolean();
				cube.setEmpty(isEmpty);
				
				if (!isEmpty) {
					ChunkSection storage = cube.getStorage();
					
					// 1. block IDs, low bits
					byte[] blockIdLsbs = new byte[16*16*16];
					in.read(blockIdLsbs);
					
					// 2. block IDs, high bits
					NibbleArray blockIdMsbs = null;
					boolean isHighBitsAttached = in.readByte() != 0;
					if (isHighBitsAttached) {
						blockIdMsbs = new NibbleArray();
						in.read(blockIdMsbs.get());
					}
					
					// 3. metadata
					NibbleArray blockMetadata = new NibbleArray();
					in.read(blockMetadata.get());
					
					ChunkSectionHelper.setBlockStates(storage, blockIdLsbs, blockIdMsbs, blockMetadata);
					
					// 4. block light
					in.read(storage.getBlockLightArray().get());
					
					if (!this.world.dimension.hasNoSky()) {
						// 5. sky light
						in.read(storage.getSkyLightArray().get());
					}
					
					storage.countBlocksInSection();
				}
				
				// flag cube for render update
				cube.markForRenderUpdate();
			}
			
			if (isFirstTime) {
				// 6. biomes
				in.read(getBiomeMap());
			}
			
			// 7. light index
			getLightIndex().readData(in);
			
			in.close();
		} catch (IOException ex) {
			log.error("Unable to read data for column ({},{})", this.chunkX, this.chunkZ, ex);
		}
		
		// update lighting flags
		this.terrainPopulated = true;
		
		// update tile entities in each chunk
		for (Cube cube : this.cubes.values()) {
			for (BlockEntity blockEntity : cube.getBlockEntities()) {
				blockEntity.updateContainingBlockInfo();
			}
		}
	}
	
	private void unloadCubes(byte[] data) {
		
		ByteArrayInputStream buf = new ByteArrayInputStream(data);
		DataInputStream in = new DataInputStream(buf);
		
		ICubeCache cubeCache = WorldContext.get(this.world).getCubeCache();
		
		try {
			// how many cubes are we reading?
			int numCubes = in.readUnsignedShort();
			for (int i = 0; i < numCubes; i++) {
				int cubeY = in.readInt();
				cubeCache.unloadCube(this.chunkX, cubeY, this.chunkZ);
			}
			in.close();
		} catch (IOException ex) {
			log.error("Unable to read data for column ({},{})", this.chunkX, this.chunkZ, ex);
		}
	}
	
	/**
	 * This method retrieves the biome at a set of coordinates
	 */
	@Override
	public Biome getBiome(BlockPos pos, BiomeManager biomeManager) {
		
		int biomeID = this.biomeMap[Coords.blockToLocal(pos.getZ()) << 4 | Coords.blockToLocal(pos.getX())] & 255;
		
		if (biomeID == 255) {
			Biome biome = biomeManager.getBiome(pos);
			biomeID = biome.biomeID;
			this.biomeMap[Coords.blockToLocal(pos.getZ()) << 4 | Coords.blockToLocal(pos.getX())] = (byte) (biomeID & 255);
		}
		
		return Biome.getBiome(biomeID) == null ? Biome.PLAINS : Biome.getBiome(biomeID);
	}
	
	@Override
	public boolean isPopulated() {
		boolean isAnyCubeLive = false;
		for (Cube cube : this.cubes.values()) {
			isAnyCubeLive |= cube.getGeneratorStage().isLastStage();
		}
		return this.ticked && this.terrainPopulated && isAnyCubeLive;
	}
	
	@Override
	public void tickChunk(boolean tryToTickFaster) {
		this.ticked = true;
		
		// don't need to do anything else here
		// lighting is handled elsewhere now
	}
	
	@Override
	@Deprecated
	public void generateSkylightMap() {
		// don't call this, use the lighting manager
		throw new UnsupportedOperationException();
	}
	
	public void resetPrecipitationHeight() {
		// init the rain map to -999, which is a kind of null value
		// this array is actually a cache
		// values will be calculated by the getter
		for (int localX = 0; localX < 16; localX++) {
			for (int localZ = 0; localZ < 16; localZ++) {
				int xzCoord = localX | localZ << 4;
				this.rainfallMap[xzCoord] = -999;
			}
		}
	}
	
	@Override
	public BlockPos getRainfallHeight(BlockPos pos) {
		
		// TODO: update this calculation to use better data structures
		
		int x = Coords.blockToLocal(pos.getX());
		int z = Coords.blockToLocal(pos.getZ());
		
		int xzCoord = x | z << 4;
		int height = this.rainfallMap[xzCoord];
		if (height == -999) {
			
			/* TODO: compute a new rain height
			look over the blocks in the top filled cube (if one exists) and do something like this:
			int maxBlockY = getTopFilledSegment() + 15;
			int minBlockY = Coords.cubeToMinBlock( getBottomCubeY() );
			Block block = cube.getBlock( ... );
			Material material = block.getMaterial();
			if( material.blocksMovement() || material.isLiquid() ) {
				height = maxBlockY + 1;
			}
			*/
			
			// TEMP: just rain down to the sea
			this.rainfallMap[xzCoord] = this.world.getSeaLevel();
		}
		
		return new BlockPos(pos.getX(), height, pos.getZ());
	}
	
	@Override
	public int getBrightestLight(BlockPos pos, int skyLightDampeningTerm) {
		// NOTE: this is called by WorldRenderers
		
		// pass off to cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			return cube.getBrightestLight(pos, skyLightDampeningTerm);
		}
		
		// defaults
		if (!this.world.dimension.hasNoSky() && skyLightDampeningTerm < LightType.SKY.defaultValue) {
			return LightType.SKY.defaultValue - skyLightDampeningTerm;
		}
		return 0;
	}
	
	@Override
	public int getLightAt(LightType lightType, BlockPos pos) {
		// NOTE: this is the light function that is called by the rendering code on client
		
		// pass off to cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			return cube.getLightValue(lightType, pos);
		}
		
		// there's no cube, rely on defaults
		if (lightType == LightType.SKY) {
			if (canSeeSky(pos)) {
				return lightType.defaultValue;
			} else {
				return 0;
			}
		}
		return 0;
	}
	
	@Override
	public void setLightAt(LightType lightType, BlockPos pos, int light) {
		
		// pass off to cube
		int cubeY = Coords.blockToCube(pos.getY());
		Cube cube = this.cubes.get(cubeY);
		if (cube != null) {
			cube.setLightValue(lightType, pos, light);
			this.isModified = true;
		}
	}
	
	protected List<RangeInt> getRanges(Iterable<Integer> yValues) {
		
		// compute a kind of run-length encoding on the cube y-values
		List<RangeInt> ranges = new ArrayList<RangeInt>();
		Integer start = null;
		Integer stop = null;
		for (int cubeY : yValues) {
			if (start == null) {
				// start a new range
				start = cubeY;
				stop = cubeY;
			} else if (cubeY == stop + 1) {
				// extend the range
				stop = cubeY;
			} else {
				// end the range
				ranges.add(new RangeInt(start, stop));
				
				// start a new range
				start = cubeY;
				stop = cubeY;
			}
		}
		
		if (start != null) {
			// finish the last range
			ranges.add(new RangeInt(start, stop));
		}
		
		return ranges;
	}
	
	@Override
	public void resetRelightChecks() {
		this.roundRobinLightUpdatePointer = 0;
		this.roundRobinCubes.clear();
		this.roundRobinCubes.addAll(this.cubes.values());
	}
	
	@Override
	public void processRelightChecks() {
		
		if (this.roundRobinCubes.isEmpty()) {
			resetRelightChecks();
		}
		
		// we get just a few updates this time
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int i = 0; i < 2; i++) {
			
			// once we've checked all the blocks, stop checking
			int maxPointer = 16 * 16 * this.roundRobinCubes.size();
			if (this.roundRobinLightUpdatePointer >= maxPointer) {
				break;
			}
			
			// get this update's arguments
			int cubeIndex = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 8);
			int localX = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 4);
			int localZ = Bits.unpackUnsigned(this.roundRobinLightUpdatePointer, 4, 0);
			
			// advance to the next block
			// this pointer advances over segment block columns
			// starting from the block columns in the bottom segment and moving upwards
			this.roundRobinLightUpdatePointer++;
			
			// get the cube that was pointed to
			Cube cube = this.roundRobinCubes.get(cubeIndex);
			
			int blockX = Coords.localToBlock(this.chunkX, localX);
			int blockZ = Coords.localToBlock(this.chunkZ, localZ);
			
			// for each block in this segment block column...
			for (int localY = 0; localY < 16; ++localY) {
				
				int blockY = Coords.localToBlock(cube.getY(), localY);
				pos.setBlockPos(blockX, blockY, blockZ);
				
				if (cube.getBlockState(pos).getBlock().getMaterial() == Material.AIR) {
					
					// if there's a light source next to this block, update the light source
					for (Facing facing : Facing.values()) {
						BlockPos neighborPos = pos.addDirection(facing, 1);
						if (this.world.getBlockStateAt(neighborPos).getBlock().getBrightness() > 0) {
							this.world.updateLightingAt(neighborPos);
						}
					}
					
					// then update this block
					this.world.updateLightingAt(pos);
				}
			}
		}
	}
	
	public void doRandomTicks() {
		if (isEmpty()) {
			return;
		}
		
		for (Cube cube : this.cubes.values()) {
			cube.doRandomTicks();
		}
	}
}
