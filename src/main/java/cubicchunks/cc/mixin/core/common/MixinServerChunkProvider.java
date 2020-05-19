package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.chunk.ticket.ITicketManager;
import cubicchunks.cc.server.IServerChunkProvider;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerChunkProvider.class)
public class MixinServerChunkProvider implements IServerChunkProvider {
    @Shadow TicketManager ticketManager;

    @Override
    public <T> void registerTicket(TicketType<T> type, SectionPos pos, int distance, T value) {
        ((ITicketManager)this.ticketManager).register(type, pos, distance, value);
    }
}
