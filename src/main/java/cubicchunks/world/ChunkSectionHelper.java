/*
 *  This file is part of Tall Worlds, licensed under the MIT License (MIT).
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

import net.minecraft.block.Block;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkSectionHelper {
	
	public static byte[] getBlockLSBArray(ExtendedBlockStorage storage) {
		byte[] out = new byte[16 * 16 * 16];
		
		char[] data = storage.getData();
		for (int i = 0; i<data.length; i++) {
			final int val = data[i];
			
			// lowest 4 bits are metadata, so shift them off
			// take the next 8 bits as block id LSBs
			out[i] = (byte)((val >> 4) & 0xff);
		}
		
		return out;
	}
	
	public static NibbleArray getBlockMSBArray(ExtendedBlockStorage storage) {
		NibbleArray out = null;
		
		char[] data = storage.getData();
		for (int i = 0; i<data.length; i++) {
			final int val = data[i];
			
			// do we have high bits?
			if (val >> 12 != 0) {
				if (out == null) {
					out = new NibbleArray();
				}
				int x = i & 0xf;
				int y = (i >> 8) & 0xf;
				int z = (i >> 4) & 0xf;
				
				// the 4 high bits are block id MSBs
				out.set(x, y, z, val >> 12);
			}
		}
		
		return out;
	}
	
	public static NibbleArray getBlockMetaArray(ExtendedBlockStorage storage) {
		NibbleArray out = new NibbleArray();
		
		char[] data = storage.getData();
		for (int i = 0; i<data.length; i++) {
			final int val = data[i];
			
			int x = i & 0xf;
			int y = (i >> 8) & 0xf;
			int z = (i >> 4) & 0xf;
			
			// the 4 low bits are metadata
			out.set(x, y, z, val & 0xf);
		}
		
		return out;
	}

	public static void setBlockStates(ExtendedBlockStorage chunkSection, byte[] blockIdLsbs, NibbleArray blockIdMsbs, NibbleArray blockMetadata) {
		for (int i=0; i<blockIdLsbs.length; i++) {
			
			// get the block
			int blockId = blockIdLsbs[i] & 0xFF;
			if (blockIdMsbs != null) {
				blockId |= (blockIdMsbs.getFromIndex(i) << 8);
			}
			Block block = Block.getBlockById(blockId);
			
			// get the metadata
			int meta = blockMetadata.getFromIndex(i);

			// save it
			int x = i & 0xf;
			int y = (i >> 8) & 0xf;
			int z = (i >> 4) & 0xf;
			chunkSection.set(x, y, z, block.getStateFromMeta(meta));
		}
	}
	
	public static byte[] getBlockIDArray(final ExtendedBlockStorage storage) {
		final char[] data = storage.getData();
		byte[] out = new byte[16 * 16 * 16 * 2];

		int byteIndex;
		int charIndex;

		charIndex = byteIndex = 0;

		for (; charIndex < data.length;) {
			// shift off the metadata
			final int val = (data[charIndex] >> 4);
			
			out[byteIndex] = (byte)(val & 0xff);
			out[byteIndex + 1] = (byte)(val >> 8);

			charIndex += 1;
			byteIndex += 2;
		}

		return out;
	}

	public static int[] byteArrayToIntArray(final byte[] in) {
		int out[] = new int[16 * 16 * 16];

		int byteIndex;
		int intIndex;

		intIndex = byteIndex = 0;

		for (; byteIndex < in.length;) {
			
			final int msb = in[byteIndex] & 0xFF;
			final int lsb = in[byteIndex + 1] << 8;

			out[intIndex] = lsb | msb;

			byteIndex += 2;
			intIndex += 1;
		}

		return out;
	}

	public static void setBlockStates(final ExtendedBlockStorage chunkSection, final int[] blockIDs,
			final NibbleArray blockMetadata) {
		for (int i = 0; i < blockIDs.length; i++) {
			// get the block
			final Block block = Block.getBlockById(blockIDs[i]);

			// get the metadata
			final int meta = blockMetadata.getFromIndex(i);

			// save it
			final int x = i & 0xf;
			final int y = (i >> 8) & 0xf;
			final int z = (i >> 4) & 0xf;
			chunkSection.set(x, y, z, block.getStateFromMeta(meta));
		}
	}
}
