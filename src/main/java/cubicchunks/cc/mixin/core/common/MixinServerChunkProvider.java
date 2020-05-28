package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.chunk.IChunkManager;
import cubicchunks.cc.chunk.ICubeHolder;
import cubicchunks.cc.chunk.cube.Cube;
import cubicchunks.cc.chunk.ticket.ITicketManager;
import cubicchunks.cc.server.IServerChunkProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ServerChunkProvider.class)
public class MixinServerChunkProvider implements IServerChunkProvider {
    @Shadow TicketManager ticketManager;
    @Shadow ChunkManager chunkManager;

    @Shadow @Final public ServerWorld world;

    @Override
    public <T> void registerTicket(TicketType<T> type, SectionPos pos, int distance, T value) {
        ((ITicketManager) this.ticketManager).register(type, pos, distance, value);
    }

    @Override public int getLoadedSectionsCount() {
        return ((IChunkManager) chunkManager).getLoadedSectionsCount();
    }

    /**
     * @author Barteks2x
     * @reason sections
     */
    @Overwrite
    public void markBlockChanged(BlockPos pos) {
        int x = pos.getX() >> 4;
        int y = pos.getX() >> 4;
        int z = pos.getZ() >> 4;
        ChunkHolder chunkholder = ((IChunkManager) this.chunkManager).getCubeHolder(SectionPos.asLong(x, y, z));
        if (chunkholder != null) {
            chunkholder.markBlockChanged(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        }
    }

    @Inject(method = "tickChunks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkManager;getLoadedChunksIterable()Ljava/lang/Iterable;"))
    private void tickSections(CallbackInfo ci) {

        ((IChunkManager) this.chunkManager).getLoadedSectionsIterable().forEach((cubeHolder) -> {
            Optional<ChunkSection> optional =
                    ((ICubeHolder) cubeHolder).getSectionEntityTickingFuture().getNow(ICubeHolder.UNLOADED_SECTION).left();
            if (optional.isPresent()) {
                Cube section = (Cube) optional.get();
                this.world.getProfiler().startSection("broadcast");
                ((ICubeHolder) cubeHolder).sendChanges(section);
                this.world.getProfiler().endSection();
            }
        });
    }
}
