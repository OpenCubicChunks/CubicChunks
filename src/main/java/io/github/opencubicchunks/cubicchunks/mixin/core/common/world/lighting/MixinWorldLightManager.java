package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.world.lighting.ILightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.IWorldLightManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.lighting.ILightListener;
import net.minecraft.world.lighting.LightEngine;
import net.minecraft.world.lighting.WorldLightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(WorldLightManager.class)
public abstract class MixinWorldLightManager implements IWorldLightManager, ILightListener {

    @Shadow public void checkBlock(BlockPos pos) { throw new Error("Mixin failed to apply correctly"); }

    @Shadow public void setData(LightType type, SectionPos pos, @Nullable NibbleArray array) { throw new Error("Mixin failed to apply correctly"); }

    @Shadow @Final @Nullable private LightEngine<?, ?> blockLight;

    @Shadow @Final @Nullable private LightEngine<?, ?> skyLight;

    @Shadow public void updateSectionStatus(SectionPos pos, boolean isEmpty) { }

    @Override
    public void retainData(CubePos cubePos, boolean retain) {
        if (this.blockLight != null) {
            ((ILightEngine)this.blockLight).retainCubeData(cubePos, retain);
        }

        if (this.skyLight != null) {
            ((ILightEngine)this.blockLight).retainCubeData(cubePos, retain);
        }
    }

    @Override
    public void enableLightSources(CubePos cubePos, boolean retain) {
        if (this.blockLight != null) {
            ((ILightEngine)this.blockLight).func_215620_a(cubePos, retain);
        }

        if (this.skyLight != null) {
            ((ILightEngine)this.skyLight).func_215620_a(cubePos, retain);
        }
    }
}
