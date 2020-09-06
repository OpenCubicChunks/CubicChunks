package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.server.Ticket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Ticket.class)
public interface TicketAccess {
    @Invoker("isExpired")boolean cc$isexpired(long currentTime);

    @Invoker("setTimestamp") void setTimestampCC(long time);
}