package cubicchunks.cc.mixin.core.common.ticket.interfaces;

import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.class)
public interface InvokeTicketManager {
    @Invoker("register") void registerCC(long chunkPosIn, Ticket<?> ticketIn);
}
