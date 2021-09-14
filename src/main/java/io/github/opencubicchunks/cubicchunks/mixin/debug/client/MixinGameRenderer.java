package io.github.opencubicchunks.cubicchunks.mixin.debug.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.opencubicchunks.cubicchunks.debug.DebugVisualization;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    private static final boolean DEBUG_WINDOW_ENABLED = System.getProperty("cubicchunks.debug.window", "false").equals("true");

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void onRender(float f, long l, PoseStack poseStack, CallbackInfo ci) {
        if (!((CubicLevelHeightAccessor) Minecraft.getInstance().level).isCubic() || !DEBUG_WINDOW_ENABLED) {
            return;
        }
        DebugVisualization.onRender();
    }
}
