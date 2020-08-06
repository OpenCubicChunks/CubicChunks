package io.github.opencubicchunks.cubicchunks.mixin.core.common.pathfinding;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.entity.MobEntity;
import net.minecraft.pathfinding.PathNavigator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PathNavigator.class)
public class MixinPathNavigator {

    @ModifyConstant(method = "func_225464_a", constant = @Constant(doubleValue = 0.0D, ordinal = 0))
    private double getPathMinPos(double _0) { return -CubicChunks.worldMAXHeight; }

}
