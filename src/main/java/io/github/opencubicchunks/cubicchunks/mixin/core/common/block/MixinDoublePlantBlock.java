package io.github.opencubicchunks.cubicchunks.mixin.core.common.block;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.level.block.DoublePlantBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(DoublePlantBlock.class)
public class MixinDoublePlantBlock {

    /**
     @author CursedFlames
     @reason Fix tall flower placement above 255
     **/
    @ModifyConstant(method = "getStateForPlacement", constant = @Constant(intValue = 255))
    private int getOutOfWorldMaxPos(int _255) { return CubicChunks.MAX_SUPPORTED_HEIGHT; }

}