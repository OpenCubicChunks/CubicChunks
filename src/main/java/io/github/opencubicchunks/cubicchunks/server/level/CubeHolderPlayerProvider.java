package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.server.level.ServerPlayer;

public interface CubeHolderPlayerProvider {
    Stream<ServerPlayer> getPlayers(CubePos cubePos, boolean onlyOnWatchDistanceEdge);
}
