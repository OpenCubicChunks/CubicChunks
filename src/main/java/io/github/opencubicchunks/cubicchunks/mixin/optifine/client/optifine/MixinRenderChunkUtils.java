package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.optifine;

import io.github.opencubicchunks.cubicchunks.mixin.access.common.ChunkSectionAccess;
import io.github.opencubicchunks.cubicchunks.optifine.IOptiFineChunkRender;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.optifine.util.RenderChunkUtils")
public class MixinRenderChunkUtils {

    @Dynamic @Inject(method = "getCountBlocks(Lnet/minecraft/client/renderer/chunk/ChunkRenderDispatcher$ChunkRender;)I",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void getCountBlocks(ChunkRenderDispatcher.RenderChunk renderChunk, CallbackInfoReturnable<Integer> cbi) {
        //if (((IOptiFineChunkRender) renderChunk).isCubic()) {
        LevelChunkSection ebs = ((IOptiFineChunkRender) renderChunk).getCube();
        int ret = ebs == null ? 0 : ((ChunkSectionAccess) ebs).getNonEmptyBlockCount();
        cbi.cancel();
        cbi.setReturnValue(ret);
        //}
    }
}
