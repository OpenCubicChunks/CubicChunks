package cubicchunks.cc.server;

import cubicchunks.cc.chunk.ICubeProvider;
import cubicchunks.cc.chunk.util.CubePos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.TicketType;

public interface IServerChunkProvider extends ICubeProvider {
    <T> void registerTicket(TicketType<T> type, CubePos pos, int distance, T value);

    int getLoadedCubesCount();

    void forceCube(CubePos pos, boolean add);
}
