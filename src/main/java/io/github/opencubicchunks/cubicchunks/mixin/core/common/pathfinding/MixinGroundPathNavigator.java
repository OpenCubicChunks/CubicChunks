package io.github.opencubicchunks.cubicchunks.mixin.core.common.pathfinding;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.pathfinding.GroundPathNavigator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GroundPathNavigator.class)
public class MixinGroundPathNavigator {

    //TODO Should check if the area is loaded! https://github.com/OpenCubicChunks/CubicChunks/blob/MC_1.12/src/main/java/io/github/opencubicchunks/cubicchunks/core/asm/mixin/fixes/common/MixinPathNavigateGround.java
    @ModifyConstant(method = "getPathToPos", constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_ZERO))
    private int getMinWorldPos(int _0) { return -CubicChunks.worldMAXHeight; }

}
