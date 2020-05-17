package cubicchunks.cc.mixin.core.common.ticket;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Either;
import cubicchunks.cc.mixin.core.common.CCTicketType;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkTaskPriorityQueueSorter;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(TicketManager.class)
public class MixinTicketManager {
    @Final @Shadow private final TicketManager.PlayerChunkTracker playerChunkTracker = new TicketManager.PlayerChunkTracker(8);

    @Shadow @Final private final TicketManager.PlayerTicketTracker playerTicketTracker = new TicketManager.PlayerTicketTracker(33);

    @Shadow @Final private final TicketManager.ChunkTicketTracker ticketTracker = new TicketManager.ChunkTicketTracker();

    @Shadow @Final private final Set<ChunkHolder> chunkHolders = Sets.newHashSet();

    @Shadow @Final private final LongSet field_219387_o = new LongOpenHashSet();

    @Shadow @Final private Executor field_219388_p;

    @Shadow @Final private  ITaskExecutor<ChunkTaskPriorityQueueSorter.RunnableEntry> field_219386_n;



    @Inject(method = "processUpdates(Lnet/minecraft/world/server/ChunkManager;)Z", at = @At("HEAD"))
    private void processUpdates(ChunkManager chunkManager, CallbackInfoReturnable<Boolean> cir) {
        this.playerChunkTracker.processAllUpdates();
        this.playerTicketTracker.processAllUpdates();
        int i = Integer.MAX_VALUE - this.ticketTracker.func_215493_a(Integer.MAX_VALUE);
        boolean flag = i != 0;
        if (flag) {
            ;
        }

        if (!this.chunkHolders.isEmpty()) {
            this.chunkHolders.forEach((chunkHolder) -> {
                ((IMixinChunkHolder)chunkHolder).processUpdates(chunkManager);
            });
            this.chunkHolders.clear();
            cir.setReturnValue(true);
        } else {
            if (!this.field_219387_o.isEmpty()) {
                LongIterator longiterator = this.field_219387_o.iterator();

                while(longiterator.hasNext()) {
                    long nextLong = longiterator.nextLong();
                    if (((IMixinTicketManager)this).getTicketSet(nextLong).stream().anyMatch((p_219369_0_) -> p_219369_0_.getType() == CCTicketType.CCPLAYER)) {
                        ChunkHolder chunkholder = ((IChunkManager)chunkManager).func_219220_a(nextLong);
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<Either<Chunk, ChunkHolder.IChunkLoadingError>> completablefuture = chunkholder.getEntityTickingFuture();
                        completablefuture.thenAccept((either) -> {
                            this.field_219388_p.execute(() -> {
                                this.field_219386_n.enqueue(ChunkTaskPriorityQueueSorter.func_219073_a(() -> {
                                }, nextLong, false));
                            });
                        });
                    }
                }

                this.field_219387_o.clear();
            }

            cir.setReturnValue(flag);
        }
    }
}
