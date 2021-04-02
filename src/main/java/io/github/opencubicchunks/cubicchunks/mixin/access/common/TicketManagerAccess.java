package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.Ticket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DistanceManager.class)
public interface TicketManagerAccess {
    @Invoker void invokeAddTicket(long chunkPosIn, Ticket<?> ticketIn);

    @Invoker void invokeUpdatePlayerTickets(int viewDistance);
}