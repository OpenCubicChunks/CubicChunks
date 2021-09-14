package io.github.opencubicchunks.cubicchunks.world.level.chunk.storage;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunkSection;

public interface PoiDeserializationContext {

    void checkConsistencyWithBlocksForCube(SectionPos chunkPos, LevelChunkSection levelChunkSection);
}
