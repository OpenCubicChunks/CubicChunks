package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.graph.CCTicketType;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TicketManager.PlayerTicketTracker.class)
public class MixinTicketManagerPlayerTicketTracker {



    @Redirect(method = "func_215504_a", at = @At(value = "FIELD", target = "Lnet/minecraft/world/server/TicketType;PLAYER:Lnet/minecraft/world/server/TicketType;"))
    private TicketType<?> getCCTicketType() {
        return CCTicketType.CCPLAYER;
    }
}
