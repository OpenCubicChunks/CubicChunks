package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import io.github.opencubicchunks.cubicchunks.world.gen.structure.ICubicStructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(StructureFeatureConfiguration.class)
public abstract class MixinStructureFeatureConfiguration implements ICubicStructureFeatureConfiguration {

    @Shadow public abstract int spacing();

    @Shadow public abstract int separation();

    @Override public int ySpacing() {
        return spacing();
    }

    @Override public int ySeparation() {
        return separation();
    }
}
