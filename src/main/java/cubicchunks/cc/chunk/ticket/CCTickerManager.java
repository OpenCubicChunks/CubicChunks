package cubicchunks.cc.chunk.ticket;

import cubicchunks.cc.mixin.core.common.ticket.interfaces.ICCTicketManager;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;

public class CCTickerManager  {

    public <T> void register(TicketType<T> type, SectionPos pos, int distance, T value) {
        ((ICCTicketManager)this).registerCC(pos.asLong(), new Ticket<>(type, 33 - distance, value));
    }
}
