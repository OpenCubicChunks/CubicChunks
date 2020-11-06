package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Entity.class)
public class MixinEntity {

    @ModifyConstant(method = "baseTick", constant = @Constant(doubleValue = -64.0D))
    private double getOutOfWorldPos(double _64)
    {
        return CubicChunks.MIN_SUPPORTED_HEIGHT - 64;
    }

}