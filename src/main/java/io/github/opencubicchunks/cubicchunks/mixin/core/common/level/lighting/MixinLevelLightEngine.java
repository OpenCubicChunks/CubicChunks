package io.github.opencubicchunks.cubicchunks.mixin.core.common.level.lighting;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ColumnCubeMapGetter;
import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import io.github.opencubicchunks.cubicchunks.world.lighting.CubicLayerLightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.CubicLevelLightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.CubicSkyLightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.SkyLightColumnChecker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.lighting.LightEventListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelLightEngine.class)
public abstract class MixinLevelLightEngine implements CubicLevelLightEngine, LightEventListener, SkyLightColumnChecker {
    @Shadow @Final protected LevelHeightAccessor levelHeightAccessor;

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
            ((CubicLayerLightEngine) this.blockEngine).retainCubeData(cubePos, retain);
        }

        if (this.skyEngine != null) {
            ((CubicLayerLightEngine) this.skyEngine).retainCubeData(cubePos, retain);
        }
    }

    @Override
    public void enableLightSources(CubePos cubePos, boolean retain) {
        if (this.blockEngine != null) {
            ((CubicLayerLightEngine) this.blockEngine).enableLightSources(cubePos, retain);
        }

        if (this.skyEngine != null) {
            ((CubicLayerLightEngine) this.skyEngine).enableLightSources(cubePos, retain);
        }
    }

    protected void doSkyLightForCube(CubeAccess cube) {
        if (this.skyEngine != null) {
            ((CubicSkyLightEngine) this.skyEngine).doSkyLightForCube(cube);
        }
    }

    @Override
    public void checkSkyLightColumn(ColumnCubeMapGetter chunk, int x, int z, int oldHeight, int newHeight) {
        if (this.skyEngine != null) {
            ((SkyLightColumnChecker) skyEngine).checkSkyLightColumn(chunk, x, z, oldHeight, newHeight);
        }
    }
}