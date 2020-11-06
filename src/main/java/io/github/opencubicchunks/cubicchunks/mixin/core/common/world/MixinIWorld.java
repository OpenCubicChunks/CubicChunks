package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelAccessor.class)
public interface MixinIWorld {

    default boolean isCubicWorld()
    {
        return true;
    }

}