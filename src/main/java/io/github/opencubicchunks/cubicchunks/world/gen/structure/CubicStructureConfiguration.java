package io.github.opencubicchunks.cubicchunks.world.gen.structure;

import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CubicStructureConfiguration {

    public static final Codec<CubicStructureConfiguration> CODEC = RecordCodecBuilder.<CubicStructureConfiguration>create((instance) -> {
        return instance.group(Codec.intRange(0, 4096).fieldOf("vertical_spacing").forGetter((config) -> {
            return config.verticalSpacing;
        }), Codec.intRange(0, 4096).fieldOf("vertical_separation").forGetter((config) -> {
            return config.verticalSeparation;
        }), Codec.INT.optionalFieldOf("maxy").orElse(Optional.of(Integer.MAX_VALUE)).forGetter((config) -> {
            return config.maxYCutoff == Integer.MAX_VALUE ? Optional.empty() : Optional.of(config.maxYCutoff);
        }), Codec.INT.optionalFieldOf("miny").orElse(Optional.of(Integer.MIN_VALUE)).forGetter((config) -> {
            return config.minYCutoff == Integer.MIN_VALUE ? Optional.empty() : Optional.of(config.minYCutoff);
        })).apply(instance, CubicStructureConfiguration::new);
    }).comapFlatMap((config) -> {
        return config.verticalSpacing <= config.verticalSeparation ? DataResult.error("Vertical spacing has to be smaller than vertical separation") : DataResult.success(config);
    }, Function.identity());


    private final int verticalSpacing;
    private final int verticalSeparation;
    private final int maxYCutoff;
    private final int minYCutoff;

    public CubicStructureConfiguration(int verticalSpacing, int verticalSeparation) {
        this(verticalSpacing, verticalSeparation, Optional.empty(), Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public CubicStructureConfiguration(int verticalSpacing, int verticalSeparation, Optional<Integer> maxYCutoff, Optional<Integer> minYCutoff) {
        this.verticalSpacing = verticalSpacing;
        this.verticalSeparation = verticalSeparation;
        this.maxYCutoff = maxYCutoff.orElse(Integer.MAX_VALUE);
        this.minYCutoff = minYCutoff.orElse(Integer.MIN_VALUE);
    }

    public int getYSpacing() {
        return verticalSpacing;
    }

    public int getYSeparation() {
        return verticalSeparation;
    }

    public int getMaxY() {
        return maxYCutoff;
    }

    public int getMinY() {
        return minYCutoff;
    }
}