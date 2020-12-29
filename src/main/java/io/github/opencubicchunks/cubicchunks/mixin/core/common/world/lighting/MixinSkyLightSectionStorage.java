package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyLightSectionStorage.class)
public abstract class MixinSkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
    private MixinSkyLightSectionStorage(LightLayer lightLayer, LightChunkGetter lightChunkGetter,
                                          SkyLightSectionStorage.SkyDataLayerStorageMap dataLayerStorageMap) {
        super(lightLayer, lightChunkGetter, dataLayerStorageMap);
    }

    @Inject(method = "enableLightSources", cancellable = true,
        at = @At(value = "INVOKE", shift= At.Shift.AFTER, target="Lnet/minecraft/world/level/lighting/SkyLightSectionStorage;runAllUpdates()V"))
    private void onEnableLightSources(long columnPos, boolean enabled, CallbackInfo ci) {
        if (enabled) {
            // We handle skylight emission differently anyway, so we don't need vanilla's sky light source system
            ci.cancel();
        }
    }
}
