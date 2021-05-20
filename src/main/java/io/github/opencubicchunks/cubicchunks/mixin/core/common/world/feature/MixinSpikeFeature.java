package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import java.util.Random;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpikeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SpikeFeature.class)
public class MixinSpikeFeature {


    @Redirect(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;getMinBuildHeight()I"))
    private int useCubeMinY(ServerLevelAccessor world) {
        return !((CubicLevelHeightAccessor) world).isCubic() ? world.getMinBuildHeight() :
            Coords.cubeToMinBlock(((CubeWorldGenRegion) world).getMainCubeY());
    }

    @Redirect(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike;getHeight()I", ordinal = 0))
    private int useCubeMaxY(SpikeFeature.EndSpike endSpike, ServerLevelAccessor world, Random random, SpikeConfiguration config, SpikeFeature.EndSpike spike) {
        return /*!((CubicLevelHeightAccessor) world).isCubic() ? endSpike.getHeight() :*/
            Coords.cubeToMaxBlock(((CubeWorldGenRegion) world).getMainCubeY());
    }

    @Redirect(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike;getHeight()I", ordinal = 1))
    private int useCubeMaxY2(SpikeFeature.EndSpike endSpike, ServerLevelAccessor world, Random random, SpikeConfiguration config, SpikeFeature.EndSpike spike) {
        return /*!((CubicLevelHeightAccessor) world).isCubic() ? endSpike.getHeight() : Math.min(endSpike.getHeight(),*/ Coords.cubeToMaxBlock(((CubeWorldGenRegion) world).getMainCubeY())
            /*)*/;
    }

    @Redirect(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike;getHeight()I", ordinal = 2))
    private int useCubeMaxY3(SpikeFeature.EndSpike endSpike, ServerLevelAccessor world, Random random, SpikeConfiguration config, SpikeFeature.EndSpike spike) {
        return /*!((CubicLevelHeightAccessor) world).isCubic() ? endSpike.getHeight() : Math.min(endSpike.getHeight(),*/ Coords.cubeToMaxBlock(((CubeWorldGenRegion) world).getMainCubeY())
            /*)*/;
    }

    @Redirect(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike;getHeight()I", ordinal = 3))
    private int useCubeMaxY4(SpikeFeature.EndSpike endSpike, ServerLevelAccessor world, Random random, SpikeConfiguration config, SpikeFeature.EndSpike spike) {
        return /*!((CubicLevelHeightAccessor) world).isCubic() ? endSpike.getHeight() : Math.min(endSpike.getHeight() - 1,*/ Coords.cubeToMaxBlock(((CubeWorldGenRegion) world).getMainCubeY
        ())/*)*/;
    }

    @Redirect(method = "placeSpike", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/SpikeFeature$EndSpike;getHeight()I", ordinal = 4))
    private int useCubeMaxY5(SpikeFeature.EndSpike endSpike, ServerLevelAccessor world, Random random, SpikeConfiguration config, SpikeFeature.EndSpike spike) {
        return !((CubicLevelHeightAccessor) world).isCubic() ? endSpike.getHeight() : Math.min(endSpike.getHeight(), Coords.cubeToMaxBlock(((CubeWorldGenRegion) world).getMainCubeY()));
    }
}
