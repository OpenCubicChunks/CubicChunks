package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ListTag.class)
public class MixinListTag {
    /**
     * @author CursedFlames
     * @reason Fix getLongArray using the wrong tag id and throwing an error every time you call it
     */
    // require = 0 so we don't crash if another mod fixes this, or it gets fixed in vanilla
    @ModifyConstant(method = "getLongArray", constant = @Constant(intValue = 11), require = 0)
    private int fixLongArrayId(int _11) {
        return 12;
    }
}
