package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.opencubicchunks.cubicchunks.debug.DebugVisualization;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRender(float f, long l, PoseStack poseStack, CallbackInfo ci) {
        DebugVisualization.onRender();
    }
}
