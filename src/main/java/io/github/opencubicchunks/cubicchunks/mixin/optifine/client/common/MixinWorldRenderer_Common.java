package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(WorldRenderer.class)
public class MixinWorldRenderer_Common {

    @Shadow @Final
    private Minecraft mc;

    @Redirect(method = "setupTerrain", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/renderer/WorldRenderer;renderDistanceChunks:I"),
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/WorldRenderer;loadRenderers()V")))
    private int setUpterrain(WorldRenderer _this) {
//        if (!((ICubicWorld) world).isCubicWorld()) {
//            return mc.gameSettings.renderDistanceChunks;
//        }
        return mc.gameSettings.renderDistanceChunks;
    }

    @ModifyConstant(method = "renderWorldBorder", constant = @Constant(intValue = 256))
    private static int modifyrenderWorldBorder(int original) {
        return 512;
    }
}
