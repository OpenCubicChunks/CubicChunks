package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TicketManager.class)
public interface ITicketManager {
//        @Invoker("register") void register(long chunkPosIn, Ticket<?> ticketIn);
}
