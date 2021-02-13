package io.github.opencubicchunks.cubicchunks.mixin.core.client.debug;

import java.util.function.Supplier;

import io.github.opencubicchunks.cubicchunks.debug.DebugVisualization;
import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientWorld extends Level {

    protected MixinClientWorld(WritableLevelData writableLevelData,
                               ResourceKey<Level> resourceKey, DimensionType dimensionType,
                               Supplier<ProfilerFiller> supplier, boolean bl, boolean bl2, long l) {
        super(writableLevelData, resourceKey, dimensionType, supplier, bl, bl2, l);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onClientWorldConstruct(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData,
                                        ResourceKey<net.minecraft.world.level.Level> resourceKey, DimensionType dimensionType, int i, Supplier<ProfilerFiller> supplier,
                                        LevelRenderer levelRenderer, boolean bl, long l, CallbackInfo ci) {

        if (((CubicLevelHeightAccessor) this).isCubic()) {
            DebugVisualization.onWorldLoad(this);
        }
    }
}
