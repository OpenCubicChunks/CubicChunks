package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.Set;

import net.minecraft.world.level.chunk.LevelChunkSection;

public interface ChunkActiveSections {

    Set<LevelChunkSection> activeSections();
}
