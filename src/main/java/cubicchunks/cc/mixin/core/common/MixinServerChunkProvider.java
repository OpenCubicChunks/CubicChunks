package cubicchunks.cc.mixin.core.common;

import net.minecraft.world.server.ServerChunkProvider;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerChunkProvider.class)
public class MixinServerChunkProvider {

    //Make this Section/cube future.
//    @Inject(method = "func_217233_c(IILnet/minecraft/world/chunk/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;", at = @At("HEAD"))
//    private void chunkFuture(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>>> cir) {
//
//    }
//
//    //Make this get Section/cube.
//    @Inject(method = "getChunk(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/IChunk;", at = @At("HEAD"))
//    private void getChunk(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load, CallbackInfoReturnable<IChunk> cir) {
//
//    }
}
