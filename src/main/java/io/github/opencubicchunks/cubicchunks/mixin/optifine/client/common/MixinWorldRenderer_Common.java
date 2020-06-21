package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.common;

import io.github.opencubicchunks.cubicchunks.CubicChunksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.WorldRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer_Common {

    @Shadow @Final
    private Minecraft mc;

    @Shadow
    private int renderDistanceChunks;

    @Shadow public abstract void loadRenderers();

    private int verticalRenderDistanceCubes = -1;

    /**
     * @author AidanLovelace
     * @reason Make sure we load the renderers again if the vertical view distance changed but not if the normal render distance changed because then they'll be loaded again anyways.
     */
    @Inject(method = "setupTerrain", at = @At("HEAD"))
    private void setupTerrainHEAD(CallbackInfo ci) {
        if (this.renderDistanceChunks != mc.gameSettings.renderDistanceChunks) return;
        if (this.verticalRenderDistanceCubes != CubicChunksConfig.verticalViewDistance.get()) {
            this.loadRenderers();
        }
    }

    @Inject(method = "loadRenderers", at = @At("HEAD"))
    private void loadRenderersHEAD(CallbackInfo ci) {
        this.verticalRenderDistanceCubes = CubicChunksConfig.verticalViewDistance.get();
    }

    @ModifyConstant(method = "renderWorldBorder", constant = @Constant(intValue = 256))
    private static int modifyrenderWorldBorder(int original) {
        return 512;
    }
}
