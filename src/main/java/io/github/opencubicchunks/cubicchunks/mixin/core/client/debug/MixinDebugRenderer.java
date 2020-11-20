package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class MixinDebugRenderer {

    @Shadow @Final public DebugRenderer.SimpleDebugRenderer heightMapRenderer;

    @Shadow private boolean renderChunkborder;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void exposeHeightMapRenderer(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double d, double e, double f, CallbackInfo ci) {
        ci.cancel();
        if (this.renderChunkborder && !Minecraft.getInstance().showOnlyReducedInfo()) {
            this.heightMapRenderer.render(poseStack, bufferSource, d, e, f);
        }
    }
}