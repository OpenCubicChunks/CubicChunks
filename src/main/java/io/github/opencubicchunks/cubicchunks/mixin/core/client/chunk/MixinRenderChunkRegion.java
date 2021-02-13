package io.github.opencubicchunks.cubicchunks.mixin.core.client.chunk;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.client.renderer.chunk.RenderChunkRegion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderChunkRegion.class)
public abstract class MixinRenderChunkRegion implements CubicLevelHeightAccessor {


    @Shadow @Final protected Level level;

    @Override public WorldStyle worldStyle() {
        return ((CubicLevelHeightAccessor) this.level).worldStyle();
    }
}
