package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// FIXME cubic checks for everything in this class
@Mixin(SkyLightSectionStorage.class)
public abstract class MixinSkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
    private MixinSkyLightSectionStorage(LightLayer lightLayer, LightChunkGetter lightChunkGetter,
                                          SkyLightSectionStorage.SkyDataLayerStorageMap dataLayerStorageMap) {
        super(lightLayer, lightChunkGetter, dataLayerStorageMap);
    }

    @Inject(method = "getLightValue(JZ)I", cancellable = true, at = @At("HEAD"))
    private void onGetLightValue(long blockPos, boolean cached, CallbackInfoReturnable<Integer> cir) {
        // Replace this method with an equivalent of BlockLightSectionStorage.getLightValue,
        // since we don't need sky light logic
        long l = SectionPos.blockToSection(blockPos);
        DataLayer dataLayer = this.getDataLayer(l, cached);
        cir.setReturnValue(dataLayer == null ? 0 : dataLayer.get(
                SectionPos.sectionRelative(BlockPos.getX(blockPos)),
                SectionPos.sectionRelative(BlockPos.getY(blockPos)),
                SectionPos.sectionRelative(BlockPos.getZ(blockPos))));
    }

    @Inject(method = "onNodeAdded", cancellable = true, at = @At("HEAD"))
    private void onOnNodeAdded(long sectionPos, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "onNodeRemoved", cancellable = true, at = @At("HEAD"))
    private void onOnNodeRemoved(long sectionPos, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "enableLightSources", cancellable = true,
        at = @At(value = "INVOKE", shift= At.Shift.AFTER, target="Lnet/minecraft/world/level/lighting/SkyLightSectionStorage;runAllUpdates()V"))
    private void onEnableLightSources(long columnPos, boolean enabled, CallbackInfo ci) {
        if (enabled) {
            // We handle skylight emission differently anyway, so we don't need vanilla's sky light source system
            ci.cancel();
        }
    }

    @Inject(method = "createDataLayer", cancellable = true, at = @At("HEAD"))
    private void onCreateDataLayer(long sectionPos, CallbackInfoReturnable<DataLayer> cir) {
        cir.setReturnValue(super.createDataLayer(sectionPos));
    }

    @Inject(method = "markNewInconsistencies", cancellable = true, at = @At("HEAD"))
    private void onMarkNewInconsistencies(LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation, CallbackInfo ci) {
        ci.cancel();
        super.markNewInconsistencies(lightProvider, doSkylight, skipEdgeLightPropagation);
    }

    @Inject(method = "hasSectionsBelow", cancellable = true, at = @At("HEAD"))
    private void onHasSectionsBelow(int sectionY, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "isAboveData", cancellable = true, at = @At("HEAD"))
    private void onIsAboveData(long sectionPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
