package cubicchunks;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ServerChunkCache;
import cubicchunks.server.ServerCubeCache;
import cuchaz.m3l.api.chunks.ChunkSystem;


public class CubicChunkSystem implements ChunkSystem {

	@Override
	public ServerChunkCache getServerChunkCache(WorldServer world) {
		
		// for now, only tall-ify the overworld
		if (world.dimension.getId() == 0) {
			return new ServerCubeCache(world);
		}
		
		return null;
	}
}
