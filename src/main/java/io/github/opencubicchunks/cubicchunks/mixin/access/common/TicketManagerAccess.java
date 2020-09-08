package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.class)
public interface TicketManagerAccess {
    @Invoker void invokeAddTicket(long chunkPosIn, Ticket<?> ticketIn);
}