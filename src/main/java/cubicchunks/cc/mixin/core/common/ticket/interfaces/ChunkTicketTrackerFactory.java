package cubicchunks.cc.mixin.core.common.ticket.interfaces;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.ChunkTicketTracker.class)
public interface ChunkTicketTrackerFactory {

    @Invoker("<init>")
    static TicketManager.ChunkTicketTracker construct(TicketManager ticketManager) {
       throw new Error("Mixin did not apply.");
    }
}
