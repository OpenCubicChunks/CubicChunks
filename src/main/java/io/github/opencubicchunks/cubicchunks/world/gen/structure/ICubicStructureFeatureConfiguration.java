package io.github.opencubicchunks.cubicchunks.world.gen.structure;

import javax.annotation.Nullable;

public interface ICubicStructureFeatureConfiguration {

    @Nullable
    CubicStructureConfiguration getVerticalSettings();

    void setVerticalSettings(@Nullable CubicStructureConfiguration cubicStructureConfiguration);
}
