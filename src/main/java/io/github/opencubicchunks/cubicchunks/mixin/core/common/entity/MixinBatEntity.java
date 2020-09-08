package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.entity.passive.BatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(BatEntity.class)
public class MixinBatEntity {

    @ModifyConstant(method = "customServerAiStep", constant = @Constant(intValue = 1, ordinal = 0))
    private int getUpdateAIWorldHeight(int _1) { return -CubicChunks.MAX_SUPPORTED_HEIGHT; }

}