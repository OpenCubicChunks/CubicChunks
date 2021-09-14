package io.github.opencubicchunks.cubicchunks.mixin.core.common.level;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.world.level.LevelHeightAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelHeightAccessor.class)
public interface MixinLevelHeightAccessor extends CubicLevelHeightAccessor {
}