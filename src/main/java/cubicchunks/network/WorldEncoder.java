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
package cubicchunks.network;

import cubicchunks.world.ClientOpacityIndex;
import cubicchunks.world.OpacityIndex;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import java.io.IOException;


public class WorldEncoder {

	public static void encodeCube(PacketBuffer out, Cube cube) throws IOException {
		// 1. emptiness
		out.writeBoolean(cube.isEmpty());

		if (!cube.isEmpty()) {
			ExtendedBlockStorage storage = cube.getStorage();

			// 2. block IDs and metadata
			storage.getData().write(out);

			// 3. block light
			out.writeBytes(storage.getBlocklightArray().getData());

			if (!cube.getWorld().getProvider().getHasNoSky()) {
				// 4. sky light
				out.writeBytes(storage.getSkylightArray().getData());
			}

			// 5. heightmap and bottom-block-y. Each non-empty cube has a chance to update this data.
			// trying to keep track of when it changes would be complex, so send it wil all cubes
			byte[] heightmaps = ((OpacityIndex) cube.getColumn().getOpacityIndex()).getDataForClient();
			assert heightmaps.length == 256*2*4;
			out.writeBytes(heightmaps);
		}
	}

	public static void encodeColumn(PacketBuffer out, Column column) throws IOException {
		// 1. biomes
		out.writeBytes(column.getBiomeArray());
	}

	public static void decodeColumn(PacketBuffer in, Column column) throws IOException {
		// 1. biomes
		in.readBytes(column.getBiomeArray());
	}

	public static void decodeCube(PacketBuffer in, Cube cube) {
		// if the cube came from the server, it must be live
		cube.setClientCube();

		// 1. emptiness
		boolean isEmpty = in.readBoolean();

		if (!isEmpty) {
			ExtendedBlockStorage storage = cube.getStorage();

			storage.getData().read(in);

			// 3. block light
			in.readBytes(storage.getBlocklightArray().getData());

			if (!cube.getWorld().getProvider().getHasNoSky()) {
				// 4. sky light
				in.readBytes(storage.getSkylightArray().getData());
			}

			// 5. heightmaps
			byte[] heightmaps = new byte[256*2*4];
			in.readBytes(heightmaps);
			ClientOpacityIndex coi = ((ClientOpacityIndex) cube.getColumn().getOpacityIndex());
			coi.setData(heightmaps);
			//cube.initialClientSkylight();
			storage.removeInvalidBlocks();
		}
	}

	public static int getEncodedSize(Column column) {
		return column.getBiomeArray().length;
	}

	public static int getEncodedSize(Cube cube) {
		int size = 0;
		size++;//isEmpty
		if (!cube.isEmpty()) {
			ExtendedBlockStorage storage = cube.getStorage();
			size += storage.getData().getSerializedSize();
			size += storage.getBlocklightArray().getData().length;
			if (!cube.getWorld().getProvider().getHasNoSky()) {
				size += storage.getSkylightArray().getData().length;
			}
			//heightmaps
			size += 256*2*4;
		}
		return size;
	}

	public static ByteBuf createByteBufForWrite(byte[] data) {
		ByteBuf bytebuf = Unpooled.wrappedBuffer(data);
		bytebuf.writerIndex(0);
		return bytebuf;
	}

	public static ByteBuf createByteBufForRead(byte[] data) {
		ByteBuf bytebuf = Unpooled.wrappedBuffer(data);
		bytebuf.readerIndex(0);
		return bytebuf;
	}
}
