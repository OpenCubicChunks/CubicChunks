package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldGenRegion.class)
public class MixinWorldGenRegion implements CubicLevelHeightAccessor {


    @Shadow @Final private ServerLevel level;

    @Override public WorldStyle worldStyle() {
        return ((CubicLevelHeightAccessor) this.level).worldStyle();
    }
}
