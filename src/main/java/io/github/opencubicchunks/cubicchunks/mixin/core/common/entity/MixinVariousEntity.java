package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = {AbstractMinecartEntity.class, BoatEntity.class})
public class MixinVariousEntity {

    /**
     @author OverInfrared
     @reason Fix boats and minecarts
    **/
    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = -64.0D))
    private double getOutOfWorldPos(double _64) { return -CubicChunks.worldMAXHeight - 64; }

    @ModifyConstant(method = "checkInWater", constant = @Constant(doubleValue = Double.MIN_VALUE))
    private double waterLevelMinValue(double orig) {
        return Double.NEGATIVE_INFINITY;
    }

}
