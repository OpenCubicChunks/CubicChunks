package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TicketManager.PlayerTicketTracker.class)
public class MixinPlayerTicketTracker {
    @Inject(method = "func_215504_a(JIZZ)V", at = @At("HEAD"), cancellable = true)
    private void ticketUpdate(long chunkPosIn, int p_215504_3_, boolean p_215504_4_, boolean p_215504_5_, CallbackInfo ci) {

    }
}
