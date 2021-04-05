package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk.storage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import com.mojang.datafixers.DataFixer;
import io.github.opencubicchunks.cubicchunks.chunk.ImposterChunkPos;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.storage.RegionCubeIO;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityStorage.class)
public class MixinEntityStorage {

    private RegionCubeIO cubeWorker;


    @Inject(method = "<init>", at = @At("RETURN"))
    private void setupCubeIO(ServerLevel serverLevel, File file, DataFixer dataFixer, boolean bl, Executor executor, CallbackInfo ci) throws IOException {
        if (((CubicLevelHeightAccessor) serverLevel).isCubic()) {
            cubeWorker = new RegionCubeIO(file, file.getName(), file.getName());
        }
    }

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
