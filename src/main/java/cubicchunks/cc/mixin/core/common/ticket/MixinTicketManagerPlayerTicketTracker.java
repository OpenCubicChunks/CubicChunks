//package cubicchunks.cc.mixin.core.common.ticket;
//
//import net.minecraft.util.math.ChunkPos;
//import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
//import net.minecraft.world.server.Ticket;
//import net.minecraft.world.server.TicketManager;
//import net.minecraft.world.server.TicketType;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(TicketManager.PlayerTicketTracker.class)
//public class MixinTicketManagerPlayerTicketTracker {
//    @Inject(method = "func_215504_a(JIZZ)V", at = @At("HEAD"))
//    private void ticketUpdate(long chunkPosIn, int p_215504_3_, boolean p_215504_4_, boolean p_215504_5_, CallbackInfo ci) {
//        if (p_215504_4_ != p_215504_5_) {
//            Ticket<?> ticket = new Ticket<>(TicketType.PLAYER, TicketManager.PLAYER_TICKET_LEVEL, new ChunkPos(chunkPosIn));
//            if (p_215504_5_) {
//                TicketManager.this.field_219385_m.enqueue(ChunkTaskPriorityQueueSorter.func_219069_a(() -> {
//                    TicketManager.this.field_219388_p.execute(() -> {
//                        if (this.func_215505_c(this.getLevel(chunkPosIn))) {
//                            TicketManager.this.register(chunkPosIn, ticket);
//                            TicketManager.this.field_219387_o.add(chunkPosIn);
//                        } else {
//                            TicketManager.this.field_219386_n.enqueue(ChunkTaskPriorityQueueSorter.func_219073_a(() -> {
//                            }, chunkPosIn, false));
//                        }
//
//                    });
//                }, chunkPosIn, () -> p_215504_3_));
//            } else {
//                TicketManager.this.field_219386_n.enqueue(ChunkTaskPriorityQueueSorter.func_219073_a(() -> {
//                    TicketManager.this.field_219388_p.execute(() -> {
//                        TicketManager.this.release(chunkPosIn, ticket);
//                    });
//                }, chunkPosIn, true));
//            }
//        }
//    }
//
//}
