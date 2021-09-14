package io.github.opencubicchunks.cubicchunks.mixin.core.common.level.lighting;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LightHeightmapGetter;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.LayerLightSectionStorageAccess;
import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LightCubeGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import net.minecraft.world.level.lighting.SkyLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SkyLightSectionStorage.class)
public abstract class MixinSkyLightSectionStorage extends LayerLightSectionStorage<SkyLightSectionStorage.SkyDataLayerStorageMap> {
    private boolean isCubic;

    private MixinSkyLightSectionStorage(LightLayer lightLayer, LightChunkGetter lightChunkGetter,
                                        SkyLightSectionStorage.SkyDataLayerStorageMap dataLayerStorageMap) {
        super(lightLayer, lightChunkGetter, dataLayerStorageMap);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(LightChunkGetter lightChunkGetter, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) lightChunkGetter.getLevel()).isCubic();
    }

    @Inject(method = "getLightValue(JZ)I", cancellable = true, at = @At("HEAD"))
    private void onGetLightValue(long blockPosLong, boolean cached, CallbackInfoReturnable<Integer> cir) {
        if (!isCubic) {
            return;
        }

        // Replace this method with an equivalent of BlockLightSectionStorage.getLightValue,
        // since we don't need sky light logic
        long sectionPosLong = SectionPos.blockToSection(blockPosLong);
        DataLayer dataLayer = this.getDataLayer(sectionPosLong, cached);
        int blockX = BlockPos.getX(blockPosLong);
        int blockY = BlockPos.getY(blockPosLong);
        int blockZ = BlockPos.getZ(blockPosLong);

        if (dataLayer == null) {

            int chunkX = Coords.blockToSection(blockX);
            int chunkZ = Coords.blockToSection(blockZ);
            BlockGetter chunk = ((LayerLightSectionStorageAccess) this).getChunkSource().getChunkForLighting(chunkX, chunkZ);


            if (chunk == null) {
                // clients can have null chunks when trying to render an entity in a chunk that hasn't arrived yet
                // if a chunk were null on the server, it would cause errors during world gen
                Level level = (Level) ((LayerLightSectionStorageAccess) this).getChunkSource().getLevel();
                //assert level.isClientSide; // can be true because of mushroom generation

                // used as a default light value, eg for rendering entities that are not within an existing chunk
                cir.setReturnValue(15);
                return;
            }

            //TODO: Optimize
            BlockGetter cube = ((LightCubeGetter) ((LayerLightSectionStorageAccess) this).getChunkSource()).getCubeForLighting(
                chunkX, Coords.blockToSection(blockY), chunkZ);
            if (cube == null || !((CubeAccess) cube).getStatus().isOrAfter(ChunkStatus.LIGHT)) {
                cir.setReturnValue(0);
                return;
            }

            Heightmap lightHeightmap = ((LightHeightmapGetter) chunk).getLightHeightmap();
            int height = lightHeightmap.getFirstAvailable(SectionPos.sectionRelative(blockX), SectionPos.sectionRelative(blockZ));
            cir.setReturnValue(height <= blockY ? 15 : 0);
        } else {
            cir.setReturnValue(dataLayer.get(
                SectionPos.sectionRelative(blockX),
                SectionPos.sectionRelative(blockY),
                SectionPos.sectionRelative(blockZ)));
        }
    }

    @Inject(method = "onNodeAdded", cancellable = true, at = @At("HEAD"))
    private void onOnNodeAdded(long sectionPos, CallbackInfo ci) {
        if (!isCubic) return;
        ci.cancel();
    }

    @Inject(method = "onNodeRemoved", cancellable = true, at = @At("HEAD"))
    private void onOnNodeRemoved(long sectionPos, CallbackInfo ci) {
        if (!isCubic) return;
        ci.cancel();
    }

    @Inject(method = "enableLightSources", cancellable = true,
        at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/lighting/SkyLightSectionStorage;runAllUpdates()V"))
    private void onEnableLightSources(long columnPos, boolean enabled, CallbackInfo ci) {
        if (!isCubic) return;
        if (enabled) {
            // We handle skylight emission differently anyway, so we don't need vanilla's sky light source system
            ci.cancel();
        }
    }

    @Inject(method = "createDataLayer", cancellable = true, at = @At("HEAD"))
    private void onCreateDataLayer(long sectionPos, CallbackInfoReturnable<DataLayer> cir) {
        if (!isCubic) return;
        cir.setReturnValue(super.createDataLayer(sectionPos));
    }

    @Inject(method = "markNewInconsistencies", cancellable = true, at = @At("HEAD"))
    private void onMarkNewInconsistencies(LayerLightEngine<SkyLightSectionStorage.SkyDataLayerStorageMap, ?> lightProvider, boolean doSkylight, boolean skipEdgeLightPropagation,
                                          CallbackInfo ci) {
        if (!isCubic) return;
        ci.cancel();
        super.markNewInconsistencies(lightProvider, doSkylight, skipEdgeLightPropagation);
    }

    @Inject(method = "hasSectionsBelow", cancellable = true, at = @At("HEAD"))
    private void onHasSectionsBelow(int sectionY, CallbackInfoReturnable<Boolean> cir) {
        if (!isCubic) return;
        cir.setReturnValue(true);
    }

    @Inject(method = "isAboveData", cancellable = true, at = @At("HEAD"))
    private void onIsAboveData(long sectionPos, CallbackInfoReturnable<Boolean> cir) {
        if (!isCubic) return;
        cir.setReturnValue(false);
    }
}
