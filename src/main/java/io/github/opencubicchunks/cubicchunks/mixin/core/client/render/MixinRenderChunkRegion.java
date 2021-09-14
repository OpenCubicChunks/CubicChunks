package io.github.opencubicchunks.cubicchunks.mixin.core.client.render;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunkRegion.class)
public abstract class MixinRenderChunkRegion implements CubicLevelHeightAccessor {

    private boolean isCubic;
    private boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setCubic(Level level, int i, int j, LevelChunk[][] levelChunks, BlockPos blockPos, BlockPos blockPos2, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) level).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) level).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) level).worldStyle();
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
