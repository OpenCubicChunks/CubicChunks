package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureFeatureManager.class)
public interface StructureFeatureManagerAccess {

    @Accessor WorldGenSettings getWorldGenSettings();
}
