package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.feature.range;

import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.CubeWorldGenRegion;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

//We do this because ores take up over half of world gen load time.
@Mixin(OreFeature.class)
public class MixinOreFeature {


    @Redirect(method = "doPlace", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;isOutsideBuildHeight(I)Z"))
    private boolean skipEmptyChunks(LevelAccessor levelAccessor, int i) {
        return Coords.cubeToMinBlock(((CubeWorldGenRegion) levelAccessor).getMainCubeY()) > i && i < Coords.cubeToMaxBlock(((CubeWorldGenRegion) levelAccessor).getMainCubeY());
    }
}
