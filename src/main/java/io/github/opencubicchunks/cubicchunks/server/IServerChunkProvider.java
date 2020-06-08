package io.github.opencubicchunks.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.server.TicketType;

import javax.annotation.Nullable;

public interface IServerChunkProvider extends ICubeProvider {
    <T> void registerTicket(TicketType<T> type, CubePos pos, int distance, T value);

    int getLoadedCubesCount();

    void forceCube(CubePos pos, boolean add);

    @Nullable
    IBlockReader getCubeForLight(int sectionX, int sectionY, int sectionZ);
}
