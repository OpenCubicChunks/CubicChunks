package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.item;

import net.minecraft.entity.item.BoatEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BoatEntity.class)
public class MixinBoatEntity {

    @ModifyConstant(method = "checkInWater", constant = @Constant(doubleValue = Double.MIN_VALUE))
    private double waterLevelMinValue(double orig) {
        return Double.NEGATIVE_INFINITY;
    }

}
