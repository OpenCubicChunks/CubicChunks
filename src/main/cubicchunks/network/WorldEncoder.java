package cubicchunks.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ChunkSection;
import cubicchunks.generator.GeneratorStage;
import cubicchunks.world.ChunkSectionHelper;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;


public class WorldEncoder {
	
	public static void encodeCube(DataOutputStream out, Cube cube)
	throws IOException {
		
		// 1. emptiness
		out.writeBoolean(cube.isEmpty());
		
		if (!cube.isEmpty()) {
			ChunkSection storage = cube.getStorage();
			
			// 2. block IDs
			out.write(ChunkSectionHelper.getBlockIDArray(storage));
			
			// 3. metadata
			out.write(ChunkSectionHelper.getBlockMetaArray(storage).get());
			
			// 4. block light
			out.write(storage.getBlockLightArray().get());
			
			if (!cube.getWorld().dimension.hasNoSky) {
				// 5. sky light
				out.write(storage.getSkyLightArray().get());
			}
		}
	}

	public static void encodeColumn(DataOutputStream out, Column column)
	throws IOException {
		
		// 1. biomes
		out.write(column.getBiomeMap());
		
		// 2. light index
		column.getLightIndex().writeData(out);
	}

	public static void decodeColumn(DataInputStream in, Column column)
	throws IOException {
		
		// 1. biomes
		in.read(column.getBiomeMap());
		
		// 2. light index
		column.getLightIndex().readData(in);
	}

	public static void decodeCube(DataInputStream in, Cube cube)
	throws IOException {
			
		// if the cube came from the server, it must be live
		cube.setGeneratorStage(GeneratorStage.getLastStage());
			
		// 1. emptiness
		boolean isEmpty = in.readBoolean();
		cube.setEmpty(isEmpty);
		
		if (!isEmpty) {
			ChunkSection storage = cube.getStorage();

			// 2. block IDs
			byte[] rawBlockIds = new byte[16*16*16*2];
			in.read(rawBlockIds);
			
			int[] blockIds = ChunkSectionHelper.byteArrayToIntArray(rawBlockIds);
			
			// 3. metadata
			NibbleArray blockMetadata = new NibbleArray();
			in.read(blockMetadata.get());
			
			ChunkSectionHelper.setBlockStates(storage, blockIds, blockMetadata);
			
			// 4. block light
			in.read(storage.getBlockLightArray().get());
			
			if (!cube.getWorld().dimension.hasNoSky()) {
				// 5. sky light
				in.read(storage.getSkyLightArray().get());
			}
			
			storage.countBlocksInSection();
		}
	}
}
