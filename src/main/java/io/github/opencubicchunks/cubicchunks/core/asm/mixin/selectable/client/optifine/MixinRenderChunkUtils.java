package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineExtendedBlockStorage;
import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineRenderChunk;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.optifine.util.RenderChunkUtils")
public class MixinRenderChunkUtils {

    @Inject(method = "getCountBlocks", at = @At("HEAD"), cancellable = true)
    private static void getCountBlocks(RenderChunk renderChunk, CallbackInfoReturnable<Integer> cbi) {
        if (((IOptifineRenderChunk) renderChunk).isCubic()) {
            ExtendedBlockStorage ebs = ((IOptifineRenderChunk) renderChunk).getCube().getStorage();
            int ret = ebs == null ? 0 : ((IOptifineExtendedBlockStorage) ebs).getBlockRefCount();
            cbi.cancel();
            cbi.setReturnValue(ret);
        }
    }
}
