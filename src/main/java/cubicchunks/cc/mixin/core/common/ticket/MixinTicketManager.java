package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.graph.CCTicketType;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TicketManager.class)
public abstract class MixinTicketManager {
    @Redirect(method = "lambda$processUpdates$2(Lnet/minecraft/world/server/Ticket;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/world/server/TicketType;PLAYER:Lnet/minecraft/world/server/TicketType;"))
    private static TicketType<?> processUpdates() {
       return CCTicketType.CCPLAYER;
    }
}
