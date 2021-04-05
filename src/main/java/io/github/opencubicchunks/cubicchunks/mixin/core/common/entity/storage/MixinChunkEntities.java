package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.chunk.ImposterChunkPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkEntities.class)
public class MixinChunkEntities {


    @Inject(method = "<init>", at = @At("RETURN"))
    private void throwOnNonImposterChunkPos(ChunkPos chunkPos, List<?> list, CallbackInfo ci) {
        if (!(chunkPos instanceof ImposterChunkPos)) {
            throw new IllegalStateException(chunkPos.getClass().getSimpleName() + " was not an instanceOf " + ImposterChunkPos.class.getSimpleName());
        }
    }
}
