package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BlockPos.class)
public class MixinBlockPos {

    //TODO: This is a temporary fix to achieve taller worlds(by stopping the light engine from NPEing. Will be removed in the future.
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 30000000))
    private static int getMaxWorldSizeXZ(int size) {
        return 1000000;
    }
}
