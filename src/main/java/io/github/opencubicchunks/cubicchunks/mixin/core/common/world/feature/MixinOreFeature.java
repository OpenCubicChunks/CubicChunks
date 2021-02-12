package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//We do this because ores take up over half of world gen load time.
@Mixin(OreFeature.class)
public class MixinOreFeature {


    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void skipEmptyChunks(FeaturePlaceContext<OreConfiguration> featurePlaceContext, CallbackInfoReturnable<Boolean> cir) {
        BlockPos blockPos = featurePlaceContext.origin();
        IBigCube cube = ((ICubicWorld) featurePlaceContext.level()).getCube(blockPos);
        LevelChunkSection section = cube.getCubeSections()[Coords.blockToIndex(blockPos.getX(), blockPos.getY(), blockPos.getZ())];
        if (section == null || section.isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
