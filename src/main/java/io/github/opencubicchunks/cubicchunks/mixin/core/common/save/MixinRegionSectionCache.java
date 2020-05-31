package io.github.opencubicchunks.cubicchunks.mixin.core.common.save;

import net.minecraft.world.chunk.storage.RegionSectionCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(RegionSectionCache.class)
public class MixinRegionSectionCache {

    @ModifyConstant(method = "func_219119_a", constant = @Constant(intValue = 16))
    public int getMaxSection(int _16)
    {
        return 32;
    }

}
