package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.world.level.chunk.ColumnCubeMap;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.ColumnCubeMapGetter;
import io.github.opencubicchunks.cubicchunks.world.level.chunk.LightHeightmapGetter;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ImposterProtoChunk.class)
public class MixinImposterProtoChunk implements ColumnCubeMapGetter, LightHeightmapGetter {

    @Shadow @Final private LevelChunk wrapped;

    @Override public ColumnCubeMap getCubeMap() {
        return ((ColumnCubeMapGetter) this.wrapped).getCubeMap();
    }

    @Override public Heightmap getLightHeightmap() {
        return ((LightHeightmapGetter) this.wrapped).getLightHeightmap();
    }
}
