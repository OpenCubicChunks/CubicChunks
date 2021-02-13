package io.github.opencubicchunks.cubicchunks.mixin.core.client.chunk;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunkRegion.class)
public abstract class MixinRenderChunkRegion implements CubicLevelHeightAccessor {

    private Boolean isCubic;
    private Boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setCubic(Level level, int i, int j, LevelChunk[][] levelChunks, BlockPos blockPos, BlockPos blockPos2, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) level).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) level).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) level).worldStyle();
    }

    @Shadow @Final protected Level level;

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
