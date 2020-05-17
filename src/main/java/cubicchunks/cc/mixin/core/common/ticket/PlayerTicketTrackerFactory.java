package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.PlayerTicketTracker.class)
public interface PlayerTicketTrackerFactory {

    @Invoker("<init>")
    static TicketManager.PlayerTicketTracker construct(TicketManager ticketManager, int p_i50684_2_) {
       throw new Error("Mixin did not apply.");
    }
}
