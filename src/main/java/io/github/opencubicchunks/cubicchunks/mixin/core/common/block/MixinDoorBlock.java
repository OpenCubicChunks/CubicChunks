package io.github.opencubicchunks.cubicchunks.mixin.core.common.block;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.block.DoorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DoorBlock.class)
public class MixinDoorBlock {

    @ModifyConstant(method = "getStateForPlacement", constant = @Constant(intValue = 255))
    private int getOutOfWorldMaxPos(int _255) { return CubicChunks.worldMAXHeight; }

}
