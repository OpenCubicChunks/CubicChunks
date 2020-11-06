package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.level.block.WitherSkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(WitherSkullBlock.class)
public class MixinWitherSkeletonSkullBlock {

    @ModifyConstant(method = "canSpawnMob", constant = @Constant(intValue = 2))
    private static int getWitherSpawnCan(int _2) { return CubicChunks.MIN_SUPPORTED_HEIGHT + 2; }

    @ModifyConstant(method = "checkSpawn", constant = @Constant(ordinal = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO))
    private static int getWitherSpawnCheck(int _2) {
        return CubicChunks.MIN_SUPPORTED_HEIGHT;
    }

}