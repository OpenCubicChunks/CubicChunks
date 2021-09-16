package io.github.opencubicchunks.cubicchunks.server.level;

import java.util.stream.Stream;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.server.level.ServerPlayer;

// TODO: move this to CubeMap, this interface is unnecessary
public interface CubeHolderPlayerProvider {
    Stream<ServerPlayer> getPlayers(CubePos cubePos, boolean onlyOnWatchDistanceEdge);
}
