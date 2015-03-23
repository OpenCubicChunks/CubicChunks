package cubicchunks;

import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ServerChunkCache;
import cubicchunks.server.ServerCubeCache;
import cuchaz.m3l.api.chunks.ChunkSystem;


public class CubicChunkSystem implements ChunkSystem {

	@Override
	public ServerChunkCache getServerChunkCache(WorldServer world) {
        return new ServerCubeCache(world);
	}
}
