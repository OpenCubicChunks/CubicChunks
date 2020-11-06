package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LayerLightSectionStorage.class)
public interface SectionLightStorageAccess {

    @Invoker("updateSectionStatus") void invokeSetColumnEnabled(long p_215526_1_, boolean p_215526_3_);

}