package io.github.opencubicchunks.cubicchunks.mixin.core.common.ticket.access;

import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.class)
public interface TicketManagerAccess {
    @Invoker("register") void registerCC(long chunkPosIn, Ticket<?> ticketIn);
}
