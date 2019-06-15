package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.util.IntRange;
import io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common.MixinWorld;
import io.github.opencubicchunks.cubicchunks.core.world.ICubicWorldInternal;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.profiler.IProfiler;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public abstract class MixinClientWorld extends MixinWorld implements ICubicWorldInternal.Client {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(ClientPlayNetHandler netHandler, WorldSettings settings, DimensionType dimensionType, int i,
        IProfiler profiler, WorldRenderer renderer, CallbackInfo ci) {

        this.initCubicWorld(new IntRange(Coords.MIN_BLOCK_Y, Coords.MAX_BLOCK_Y), new IntRange(Coords.MIN_BLOCK_Y, Coords.MAX_BLOCK_Y));
    }

    @Override public void initCubicClientWorld() {
        LOGGER.info("  Applying client-specific cubic chunks world initialization for {} ({})", this.getWorldInfo().getWorldName(), this);

    }
}
