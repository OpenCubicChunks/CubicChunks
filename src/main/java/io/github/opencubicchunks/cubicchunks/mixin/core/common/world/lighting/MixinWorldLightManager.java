package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.world.lighting.ILightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.IWorldLightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelLightEngine.class)
public abstract class MixinWorldLightManager implements IWorldLightManager, LightEventListener {

    @Shadow @Final @Nullable private LayerLightEngine<?, ?> blockEngine;

    @Shadow @Final @Nullable private LayerLightEngine<?, ?> skyEngine;

    @Shadow public void checkBlock(BlockPos pos) {
        throw new Error("Mixin failed to apply correctly");
    }

    @Shadow public void queueSectionData(LightLayer type, SectionPos pos, @Nullable DataLayer array, boolean flag) {
        throw new Error("Mixin failed to apply correctly");
    }

    @Shadow public void updateSectionStatus(SectionPos pos, boolean isEmpty) {
    }

    @Shadow public void onBlockEmissionIncrease(BlockPos blockPos, int lightValue) {
    }

    @Override
    public void retainData(CubePos cubePos, boolean retain) {
        if (this.blockEngine != null) {
            ((ILightEngine) this.blockEngine).retainCubeData(cubePos, retain);
        }

        if (this.skyEngine != null) {
            ((ILightEngine) this.skyEngine).retainCubeData(cubePos, retain);
        }
    }

    @Override
    public void enableLightSources(CubePos cubePos, boolean retain) {
        if (this.blockEngine != null) {
            ((ILightEngine) this.blockEngine).enableLightSources(cubePos, retain);
        }

        if (this.skyEngine != null) {
            ((ILightEngine) this.skyEngine).enableLightSources(cubePos, retain);
        }
    }


}