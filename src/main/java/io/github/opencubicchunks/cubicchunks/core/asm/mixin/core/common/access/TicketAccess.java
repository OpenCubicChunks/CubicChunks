package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.access;

import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Ticket.class)
public interface TicketAccess {
    @SuppressWarnings("PublicStaticMixinMember")
    @Invoker("<init>")
    static <T extends Comparable<T>> Ticket<T> newTicket(TicketType<T> type, int level, T value) {
        throw new Error("Mixin not applied!");
    }
}
