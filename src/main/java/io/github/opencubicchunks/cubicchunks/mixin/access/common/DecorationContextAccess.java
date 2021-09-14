package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.placement.DecorationContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DecorationContext.class)
public interface DecorationContextAccess {

    @Accessor WorldGenLevel getLevel();
}
