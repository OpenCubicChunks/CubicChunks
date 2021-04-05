package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import io.github.opencubicchunks.cubicchunks.chunk.ImposterChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityStorage.class)
public class MixinEntityStorage {

    @Inject(method = "readChunkPos", at = @At("HEAD"), cancellable = true)
    private static void readAsCubePos(CompoundTag chunkTag, CallbackInfoReturnable<ChunkPos> cir) {
        int[] cubePosArray = chunkTag.getIntArray("Position");
        cir.setReturnValue(new ImposterChunkPos(cubePosArray[0], cubePosArray[1], cubePosArray[2]));
    }

    @Inject(method = "writeChunkPos", at = @At("HEAD"), cancellable = true)
    private static void writeAsCubePos(CompoundTag chunkTag, ChunkPos pos, CallbackInfo ci) {
        if (pos instanceof ImposterChunkPos) {
            ci.cancel();
            chunkTag.put("Position", new IntArrayTag(new int[] { pos.x, ((ImposterChunkPos) pos).y, pos.z }));
        } //else {
//            throw new IllegalStateException(pos.getClass().getSimpleName() + " was not an instanceOf " + ImposterChunkPos.class.getSimpleName());
//        }
    }
}
