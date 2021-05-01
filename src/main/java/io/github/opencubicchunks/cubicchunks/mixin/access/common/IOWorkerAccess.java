package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.concurrent.CompletableFuture;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.IOWorker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(IOWorker.class)
public interface IOWorkerAccess {

    @Invoker
    CompletableFuture<CompoundTag> invokeLoadAsync(ChunkPos pos);

}
