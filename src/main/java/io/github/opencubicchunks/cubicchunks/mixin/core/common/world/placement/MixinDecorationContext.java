package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.placement;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DecorationContext.class)
public class MixinDecorationContext implements CubicLevelHeightAccessor {

    @Shadow @Final private WorldGenLevel level;

    @Override public WorldStyle worldStyle() {
        return ((CubicLevelHeightAccessor) this.level).worldStyle();
    }
}
