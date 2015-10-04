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

import cubicchunks.generator.GeneratorStage;
import cubicchunks.util.ArrayConverter;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class WorldEncoder {
	
	public static void encodeCube(DataOutputStream out, Cube cube)
	throws IOException {
		
		// 1. emptiness
		out.writeBoolean(cube.isEmpty());
		
		if (!cube.isEmpty()) {
			ExtendedBlockStorage storage = cube.getStorage();
			
			// 2. block IDs and metadata
			out.write(ArrayConverter.toByteArray(storage.getData()));

			// 3. block light
			out.write(storage.getBlocklightArray().getData());
			
			if (!cube.getWorld().provider.getHasNoSky()) {
				// 4. sky light
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

			// 2. block IDs and metadata
			byte[] rawBlockIds = new byte[16*16*16*2];
			in.read(rawBlockIds);

			storage.setData(ArrayConverter.toCharArray(rawBlockIds));

			// 3. block light
			in.read(storage.getBlocklightArray().getData());
			
			if (!cube.getWorld().provider.getHasNoSky()) {
				// 4. sky light
				in.read(storage.getSkylightArray().getData());
			}
			
			storage.removeInvalidBlocks();
		}
	}
}
