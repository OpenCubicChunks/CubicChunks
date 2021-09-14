package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelHeightAccessor.class)
public interface MixinLevelHeightAccessor extends CubicLevelHeightAccessor {
}