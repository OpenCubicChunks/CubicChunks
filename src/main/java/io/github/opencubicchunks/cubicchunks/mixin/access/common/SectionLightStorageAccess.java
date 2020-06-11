package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.lighting.SectionLightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SectionLightStorage.class)
public interface SectionLightStorageAccess {

    @Invoker("setColumnEnabled") void invokeSetColumnEnabled(long p_215526_1_, boolean p_215526_3_);

}
