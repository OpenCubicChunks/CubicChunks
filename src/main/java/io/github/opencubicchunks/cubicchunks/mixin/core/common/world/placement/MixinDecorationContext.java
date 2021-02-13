package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecorationContext.class)
public class MixinDecorationContext implements CubicLevelHeightAccessor {

    private Boolean isCubic;
    private Boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initializeCubic(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) worldGenLevel).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) worldGenLevel).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) worldGenLevel).worldStyle();
    }

    @Override public WorldStyle worldStyle() {
                if (worldStyle == null)
            new Error().printStackTrace();
        return worldStyle;
    }

    @Override public Boolean isCubic() {
                if (isCubic == null)
            new Error().printStackTrace();
        return isCubic;
    }

    @Override public Boolean generates2DChunks() {
                if (generates2DChunks == null)
            new Error().printStackTrace();
        return generates2DChunks;
    }

}
