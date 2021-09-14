package io.github.opencubicchunks.cubicchunks.mixin.levelgen.common.placement;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DecorationContext.class)
public class MixinDecorationContext implements CubicLevelHeightAccessor {

    private boolean isCubic;
    private boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void initializeCubic(WorldGenLevel worldGenLevel, ChunkGenerator chunkGenerator, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) worldGenLevel).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) worldGenLevel).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) worldGenLevel).worldStyle();
    }

    @Override public WorldStyle worldStyle() {
        return worldStyle;
    }

    @Override public boolean isCubic() {
        return isCubic;
    }

    @Override public boolean generates2DChunks() {
        return generates2DChunks;
    }

}
