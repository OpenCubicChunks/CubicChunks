package io.github.opencubicchunks.cubicchunks.mixin.core.common.level;

import javax.annotation.Nullable;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeAccess;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.CubeSource;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LightCubeGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkSource.class)
public abstract class MixinChunkSource implements LightCubeGetter, CubeSource {

    @Override
    @Nullable
    public BlockGetter getCubeForLighting(int sectionX, int sectionY, int sectionZ) {
        return this.getCube(Coords.sectionToCube(sectionX), Coords.sectionToCube(sectionY), Coords.sectionToCube(sectionZ), ChunkStatus.EMPTY, false);
    }

    @Override
    @Nullable
    public abstract CubeAccess getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus requiredStatus, boolean load);
}