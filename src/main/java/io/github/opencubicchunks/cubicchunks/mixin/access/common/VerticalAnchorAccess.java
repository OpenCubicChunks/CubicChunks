package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.levelgen.VerticalAnchor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VerticalAnchor.class)
public interface VerticalAnchorAccess {


    @Invoker
    int invokeValue();

}
