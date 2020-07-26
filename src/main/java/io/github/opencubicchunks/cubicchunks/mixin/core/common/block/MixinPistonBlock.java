package io.github.opencubicchunks.cubicchunks.mixin.core.common.block;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.block.PistonBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PistonBlock.class)
public class MixinPistonBlock {

    @ModifyConstant(method = "canPush", constant = @Constant(ordinal = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private static int getCanPushGreater(int _0) { return -CubicChunks.worldMAXHeight; }

    @ModifyConstant(method = "canPush", constant = @Constant(ordinal = 1))
    private static int getCanPushNotEqual(int _0) { return -CubicChunks.worldMAXHeight; }

}
