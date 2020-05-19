package cubicchunks.cc.server;

import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.TicketType;

public interface IServerChunkProvider {
    public <T> void registerTicket(TicketType<T> type, SectionPos pos, int distance, T value);
}
