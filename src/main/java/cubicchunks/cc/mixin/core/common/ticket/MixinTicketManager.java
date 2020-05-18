package cubicchunks.cc.mixin.core.common.ticket;

import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TicketManager.class)
public class MixinTicketManager {
//    @Shadow
//    private long currentTime;
//
//    @Final
//    @Shadow
//    private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();
//
//
//    private final CCTicketManager.CubeTicketManager ticketTracker = new CCTicketManager.CubeTicketManager();
//    private final CCTicketManager.PlayerCubeTracker playerChunkTracker = new CCTicketManager.PlayerCubeTracker();
//    private final CCTicketManager.PlayerTicketTracker playerTicketTracker = new CCTicketManager.PlayerTicketTracker(33);
//
//
//    @Redirect(method = "lambda$processUpdates$2(Lnet/minecraft/world/server/Ticket;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/world/server/TicketType;PLAYER:Lnet/minecraft/world/server/TicketType;"))
//    private static TicketType<?> processUpdates() {
//       return CCTicketType.CCPLAYER;
//    }
//
//    @Inject(method = "tick()V", at = @At("HEAD"))
//    private void tick(CallbackInfo ci) {
//        ++this.currentTime;
//        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();
//
//        while(objectiterator.hasNext()) {
//            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
//            if (entry.getValue().removeIf((p_219370_1_) -> p_219370_1_.isExpired(this.currentTime))) {
//                this.ticketTracker.updateSourceLevel(entry.getLongKey(), func_229844_a_(entry.getValue()), false);
//            }
//
//            if (entry.getValue().isEmpty()) {
//                objectiterator.remove();
//            }
//        }
//
//    }
}
