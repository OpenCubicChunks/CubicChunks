package cubicchunks;

import cubicchunks.generator.GeneratorPipeline;
import net.minecraft.world.WorldServer;

public interface ICubicChunksWorldType {
	void registerWorldGen(WorldServer world, GeneratorPipeline pipeline);
}
