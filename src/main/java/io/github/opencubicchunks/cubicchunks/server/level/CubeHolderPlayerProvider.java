package io.github.opencubicchunks.cubicchunks.chunk;

import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.server.level.ServerPlayer;

public interface CubePlayerProvider {
    Stream<ServerPlayer> getPlayers(CubePos cubePos, boolean onlyOnWatchDistanceEdge);
}
