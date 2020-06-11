package io.github.opencubicchunks.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.server.TicketType;

public interface IServerChunkProvider extends ICubeProvider {
    <T> void registerTicket(TicketType<T> type, CubePos pos, int distance, T value);

    int getCubeLoadCounter();

    void forceCube(CubePos pos, boolean add);
}
