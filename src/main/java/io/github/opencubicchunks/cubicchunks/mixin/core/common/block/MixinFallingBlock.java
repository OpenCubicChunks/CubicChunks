package io.github.opencubicchunks.cubicchunks.mixin.core.common.block;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.block.FallingBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FallingBlock.class)
public class MixinFallingBlock {

    @ModifyConstant(method = "tick", constant = @Constant( expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private int getTickPos(int _0) { return CubicChunks.MIN_SUPPORTED_HEIGHT; }

}
