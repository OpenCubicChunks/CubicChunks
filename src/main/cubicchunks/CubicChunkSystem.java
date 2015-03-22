package cubicchunks;

import net.minecraft.world.Dimension;
import net.minecraft.world.IChunkProvider;
import net.minecraft.world.ISaveHandler;
import net.minecraft.world.WorldClient;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.storage.ChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import cuchaz.m3l.api.chunks.ChunkSystem;


public class CubicChunkSystem implements ChunkSystem {

	@Override
	public IChunkLoader getChunkLoader(ISaveHandler saveHandler, Dimension dimension) {
		// TEMP: return an ordinary chunk loader for now
		return new ChunkLoader(saveHandler.getSaveFile());
	}

	@Override
	public IChunkProvider getClientChunkProvider(WorldClient world) {
		// TODO
		return null;
	}

	@Override
	public IChunkProvider getServerChunkProvider(WorldServer world) {
		// TODO
		return null;
	}
}
