package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.graph.CCTicketType;
import cubicchunks.cc.chunk.ticket.CCTicketManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.server.Ticket;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TicketManager.class)
public class MixinTicketManager {
    @Shadow
    private long currentTime;

    @Final
    @Shadow
    private final Long2ObjectOpenHashMap<SortedArraySet<Ticket<?>>> tickets = new Long2ObjectOpenHashMap<>();


    private final CCTicketManager.CubeTicketManager ticketTracker = new CCTicketManager.CubeTicketManager();
    private final CCTicketManager.PlayerCubeTracker playerChunkTracker = new CCTicketManager.PlayerCubeTracker();
    private final CCTicketManager.PlayerTicketTracker playerTicketTracker = new CCTicketManager.PlayerTicketTracker(33);


    @Redirect(method = "lambda$processUpdates$2(Lnet/minecraft/world/server/Ticket;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/world/server/TicketType;PLAYER:Lnet/minecraft/world/server/TicketType;"))
    private static TicketType<?> processUpdates() {
       return CCTicketType.CCPLAYER;
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void tick(CallbackInfo ci) {
        ++this.currentTime;
        ObjectIterator<Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>>> objectiterator = this.tickets.long2ObjectEntrySet().fastIterator();

        while(objectiterator.hasNext()) {
            Long2ObjectMap.Entry<SortedArraySet<Ticket<?>>> entry = objectiterator.next();
            if (entry.getValue().removeIf((p_219370_1_) -> p_219370_1_.isExpired(this.currentTime))) {
                this.ticketTracker.updateSourceLevel(entry.getLongKey(), func_229844_a_(entry.getValue()), false);
            }

            if (entry.getValue().isEmpty()) {
                objectiterator.remove();
            }
        }

    }
    }
}
