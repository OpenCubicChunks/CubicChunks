package cubicchunks.cc.mixin.core.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {
    @Redirect(method = "loadInitialChunks(Lnet/minecraft/world/chunk/listener/IChunkStatusListener;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/server/ServerChunkProvider;registerTicket(Lnet/minecraft/world/server/TicketType;Lnet/minecraft/util/math/ChunkPos;ILjava/lang/Object;)V"))
    private void loadSpawnChunks(ServerChunkProvider serverChunkProvider, TicketType<?> type, ChunkPos pos, int distance, Object value) {

    }
}
