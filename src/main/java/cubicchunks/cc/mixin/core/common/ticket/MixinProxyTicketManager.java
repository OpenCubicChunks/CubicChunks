package cubicchunks.cc.mixin.core.common.ticket;

import cubicchunks.cc.chunk.ticket.ITicketManager;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.TicketManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.Executor;
/*
@Mixin(ChunkManager.ProxyTicketManager.class)
public abstract class MixinProxyTicketManager extends TicketManager implements ITicketManager {
    protected MixinProxyTicketManager(Executor p_i50707_1_, Executor p_i50707_2_) {
        super(p_i50707_1_, p_i50707_2_);
    }
}*/
