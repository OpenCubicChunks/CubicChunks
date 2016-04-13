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
package cubicchunks.world;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public class ChunkSectionHelper {
	public static final int HAS_MSB = 1<<0;
	public static final int HAS_META = 1<<1;

	public static int getBlockDataArray(ExtendedBlockStorage storage, byte[] idLSB, byte[] idMSB, byte[] metadata) {
		NibbleArray metadataNibble = new NibbleArray();
		NibbleArray idMSBNibble = storage.getData().getDataForNBT(idLSB, metadataNibble);
		int retFlags = 0;
		if(idMSBNibble != null) {
			System.arraycopy(idMSBNibble.getData(), 0, idMSB, 0, idMSB.length);
			retFlags |= HAS_MSB;
		}
		if(metadataNibble != null) {
			System.arraycopy(metadataNibble.getData(), 0, metadata, 0, metadata.length);
			retFlags |= HAS_META;
		}
		return retFlags;
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
