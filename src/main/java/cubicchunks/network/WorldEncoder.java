/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 Tall Worlds
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
package cubicchunks.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.world.chunk.NibbleArray;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.world.ChunkSectionHelper;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;


public class WorldEncoder {
	
	public static void encodeCube(DataOutputStream out, Cube cube)
	throws IOException {
		
		// 1. emptiness
		out.writeBoolean(cube.isEmpty());
		
		if (!cube.isEmpty()) {
			ExtendedBlockStorage storage = cube.getStorage();
			
			// 2. block IDs
			out.write(ChunkSectionHelper.getBlockIDArray(storage));
			
			// 3. metadata
			out.write(ChunkSectionHelper.getBlockMetaArray(storage).getData());
			
			// 4. block light
			out.write(storage.getBlocklightArray().getData());
			
			if (!cube.getWorld().provider.getHasNoSky()) {
				// 5. sky light
				out.write(storage.getSkylightArray().getData());
			}
		}
	}

	public static void encodeColumn(DataOutputStream out, Column column)
	throws IOException {
		
		// 1. biomes
		out.write(column.getBiomeArray());
		
		// 2. light index
		column.getOpacityIndex().writeData(out);
	}

	public static void decodeColumn(DataInputStream in, Column column)
	throws IOException {
		
		// 1. biomes
		in.read(column.getBiomeArray());
		
		// 2. light index
		column.getOpacityIndex().readData(in);
	}

	public static void decodeCube(DataInputStream in, Cube cube)
	throws IOException {
			
		// if the cube came from the server, it must be live
		cube.setGeneratorStage(GeneratorStage.getLastStage());
			
		// 1. emptiness
		boolean isEmpty = in.readBoolean();
		cube.setEmpty(isEmpty);
		
		if (!isEmpty) {
			ExtendedBlockStorage storage = cube.getStorage();

			// 2. block IDs
			byte[] rawBlockIds = new byte[16*16*16*2];
			in.read(rawBlockIds);
			
			int[] blockIds = ChunkSectionHelper.byteArrayToIntArray(rawBlockIds);
			
			// 3. metadata
			NibbleArray blockMetadata = new NibbleArray();
			in.read(blockMetadata.getData());
			
			ChunkSectionHelper.setBlockStates(storage, blockIds, blockMetadata);
			
			// 4. block light
			in.read(storage.getBlocklightArray().getData());
			
			if (!cube.getWorld().provider.getHasNoSky()) {
				// 5. sky light
				in.read(storage.getSkylightArray().getData());
			}
			
			storage.removeInvalidBlocks();
		}
	}
}
