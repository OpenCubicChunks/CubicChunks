package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import net.minecraft.world.IWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IWorld.class)
public interface MixinIWorld {

    default boolean isCubicWorld()
    {
        return true;
    }

}
