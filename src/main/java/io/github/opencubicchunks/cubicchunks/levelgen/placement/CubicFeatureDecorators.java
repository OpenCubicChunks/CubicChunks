package io.github.opencubicchunks.cubicchunks.levelgen.placement;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.DecoratorConfiguration;
import net.minecraft.world.level.levelgen.placement.FeatureDecorator;

public class CubicFeatureDecorators {

    public static final FeatureDecorator<CubicLakePlacementConfig> CUBIC_LAKE = featureDecorator("cubic_lake", new SurfaceProjectedDecorator(CubicLakePlacementConfig.CODEC));

    public static void init() {
    }

    public static <T extends DecoratorConfiguration> FeatureDecorator<T> featureDecorator(String ID, FeatureDecorator<T> decorator) {
        Registry.register(Registry.DECORATOR, new ResourceLocation(CubicChunks.MODID, ID), decorator);
        return decorator;
    }

}
