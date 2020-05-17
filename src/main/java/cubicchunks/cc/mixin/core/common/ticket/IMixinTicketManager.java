package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.util.SortedArraySet;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketManager.class)
public interface IMixinTicketManager {
        @Invoker("getTicketSet")
        SortedArraySet<Ticket<?>> getTicketSet(long p_229848_1_);
}
