package cubicchunks.world;

import net.minecraft.block.Block;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ChunkSection;

public class ChunkSectionHelper {
	
	public static byte[] getBlockLSBArray(ChunkSection storage) {
		byte[] out = new byte[16 * 16 * 16];
		
		char[] data = storage.getBlockDataArray();
		for (int i = 0; i<data.length; i++) {
			final int val = data[i];
			
			// lowest 4 bits are metadata, so shift them off
			// take the next 8 bits as block id LSBs
			out[i] = (byte)((val >> 4) & 0xff);
		}
		
		return out;
	}
	
	public static NibbleArray getBlockMSBArray(ChunkSection storage) {
		NibbleArray out = null;
		
		char[] data = storage.getBlockDataArray();
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
				out.setValueAtCoords(x, y, z, val >> 12);
			}
		}
		
		return out;
	}
	
	public static NibbleArray getBlockMetaArray(ChunkSection storage) {
		NibbleArray out = new NibbleArray();
		
		char[] data = storage.getBlockDataArray();
		for (int i = 0; i<data.length; i++) {
			final int val = data[i];
			
			int x = i & 0xf;
			int y = (i >> 8) & 0xf;
			int z = (i >> 4) & 0xf;
			
			// the 4 low bits are metadata
			out.setValueAtCoords(x, y, z, val & 0xf);
		}
		
		return out;
	}

	public static void setBlockStates(ChunkSection chunkSection, byte[] blockIdLsbs, NibbleArray blockIdMsbs, NibbleArray blockMetadata) {
		for (int i=0; i<blockIdLsbs.length; i++) {
			
			// get the block
			int blockId = blockIdLsbs[i];
			if (blockIdMsbs != null) {
				blockId |= (blockIdMsbs.getValue(i) << 12);
			}
			Block block = Block.getBlockFromIndex(blockId);
			
			// get the metadata
			int meta = blockMetadata.getValue(i);

			// save it
			int x = i & 0xf;
			int y = (i >> 8) & 0xf;
			int z = (i >> 4) & 0xf;
			chunkSection.setBlockStateAt(x, y, z, block.getBlockStateForMetadata(meta));
		}
	}
}
