package io.github.opencubicchunks.cubicchunks.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;

public class CubicLakePlacementConfig implements DecoratorConfiguration {

    public static final Codec<CubicLakePlacementConfig> CODEC = RecordCodecBuilder.create((instance) ->
        instance.group(
            UserFunction.CODEC.fieldOf("surface_probability").forGetter((config) -> config.surfaceProbability),
            UserFunction.CODEC.fieldOf("main_probability").forGetter((config) -> config.mainProbability)
        ).apply(instance, CubicLakePlacementConfig::new));

    private final UserFunction surfaceProbability;
    private final UserFunction mainProbability;

    public CubicLakePlacementConfig(UserFunction surfaceProbability, UserFunction mainProbability) {
        this.surfaceProbability = surfaceProbability;
        this.mainProbability = mainProbability;
    }

    public UserFunction getSurfaceProbability() {
        return surfaceProbability;
    }

    public UserFunction getMainProbability() {
        return mainProbability;
    }
}
