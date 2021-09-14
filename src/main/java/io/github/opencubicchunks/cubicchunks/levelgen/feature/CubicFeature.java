package io.github.opencubicchunks.cubicchunks.levelgen.feature;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

// TODO: make this a base class for cubic chunks features, extend Feature here
public class CubicFeature {

    public static final Feature<NoneFeatureConfiguration> LAVA_LEAK_FIX = createFeature("lava_leak_fix", new LavaLeakFix(NoneFeatureConfiguration.CODEC));

    public static <C extends FeatureConfiguration, F extends Feature<C>> F createFeature(String id, F feature) {
        ResourceLocation resourceLocation = new ResourceLocation(CubicChunks.MODID, id);
        if (Registry.FEATURE.keySet().contains(resourceLocation)) {
            throw new IllegalStateException("Feature ID: \"" + resourceLocation.toString() + "\" already exists in the Features registry!");
        }

        Registry.register(Registry.FEATURE, resourceLocation, feature);
        return feature;
    }
}
