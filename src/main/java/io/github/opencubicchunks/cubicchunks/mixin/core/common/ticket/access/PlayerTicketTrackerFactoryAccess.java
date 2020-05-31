package io.github.opencubicchunks.cubicchunks.mixin.core.common.ticket.access;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.PlayerTicketTracker.class)
public interface PlayerTicketTrackerFactoryAccess {

    @Invoker("<init>")
    static TicketManager.PlayerTicketTracker construct(TicketManager ticketManager, int p_i50684_2_) {
       throw new Error("Mixin did not apply.");
    }
}
