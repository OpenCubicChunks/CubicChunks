package io.github.opencubicchunks.cubicchunks.world.gen.structure;

import java.util.Optional;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class CubicStructureConfiguration {


    public static final Codec<CubicStructureConfiguration> CODEC = RecordCodecBuilder.<CubicStructureConfiguration>create((instance) -> {
        return instance.group(Codec.intRange(0, 4096).fieldOf("vertical_spacing").forGetter((config) -> {
            return config.vSpacing;
        }), Codec.intRange(0, 4096).fieldOf("vertical_separation").forGetter((config) -> {
            return config.vSeparation;
        }), Codec.INT.optionalFieldOf("maxy").orElse(Optional.of(Integer.MAX_VALUE)).forGetter((config) -> {
            return config.maxYCutoff == Integer.MAX_VALUE ? Optional.empty() : Optional.of(config.maxYCutoff);
        }), Codec.INT.optionalFieldOf("miny").orElse(Optional.of(Integer.MIN_VALUE)).forGetter((config) -> {
            return config.minYCutoff == Integer.MIN_VALUE ? Optional.empty() : Optional.of(config.minYCutoff);
        })).apply(instance, CubicStructureConfiguration::new);
    }).comapFlatMap((config) -> {
        return config.vSpacing <= config.vSeparation ? DataResult.error("Vertical spacing has to be smaller than vertical separation") : DataResult.success(config);
    }, Function.identity());


    private final int vSpacing;
    private final int vSeparation;
    private final int maxYCutoff;
    private final int minYCutoff;

    public CubicStructureConfiguration(int vSpacing, int vSeparation) {
        this(vSpacing, vSeparation, Optional.empty(), Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public CubicStructureConfiguration(int vSpacing, int vSeparation, Optional<Integer> maxYCutoff, Optional<Integer> minYCutoff) {
        this.vSpacing = vSpacing;
        this.vSeparation = vSeparation;
        this.maxYCutoff = maxYCutoff.orElse(Integer.MAX_VALUE);
        this.minYCutoff = minYCutoff.orElse(Integer.MIN_VALUE);
    }

    public int getYSpacing() {
        return vSpacing;
    }

    public int getYSeparation() {
        return vSeparation;
    }
}