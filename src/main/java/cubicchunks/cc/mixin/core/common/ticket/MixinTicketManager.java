package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TicketManager.class)
public class MixinTicketManager {

//    @Inject(method = "register(Lnet/minecraft/world/server/TicketType;Lnet/minecraft/util/math/ChunkPos;ILjava/lang/Object;)V", at = @At(value = "HEAD"), remap = false, cancellable = true)
//    private <T> void register(TicketType<T> type, ChunkPos pos, int distance, T value, CallbackInfo ci) {
//        ((ITicketManager)this).register(pos.asLong(), new Ticket<>(type, 33 - distance, value));
//    }
}
