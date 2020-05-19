package cubicchunks.cc.mixin.core.common.ticket.interfaces;

import net.minecraft.world.server.Ticket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Ticket.class)
public interface InvokeTicket {
    @Invoker("isExpired")boolean cc$isexpired(long currentTime);

    @Invoker("setTimestamp") void setTimestampCC(long time);
}
