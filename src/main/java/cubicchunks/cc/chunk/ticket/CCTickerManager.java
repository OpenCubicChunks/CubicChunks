package cubicchunks.cc.chunk.ticket;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

public class CCTickerManager  {
    
    public <T> void register(TicketType<T> type, SectionPos pos, int distance, T value) {
        this.register(pos.asLong(), new Ticket<>(type, 33 - distance, value));
    }
}
