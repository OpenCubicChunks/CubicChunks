package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.SectionLightStorageAccess;
import io.github.opencubicchunks.cubicchunks.world.lighting.ILightEngine;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.lighting.LightDataMap;
import net.minecraft.world.lighting.LightEngine;
import net.minecraft.world.lighting.SectionLightStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LightEngine.class)
public class MixinLightEngine <M extends LightDataMap<M>, S extends SectionLightStorage<M>> implements ILightEngine {

    @Shadow @Final protected S storage;

    @Override
    public void retainCubeData(CubePos pos, boolean retain) {
        long i = pos.asSectionPos().asLong();
        this.storage.retainChunkData(i, retain);
    }

    @Override
    public void func_215620_a(CubePos cubePos, boolean p_215620_2_) {
        ChunkPos chunkPos = cubePos.asChunkPos();
        //TODO: implement invokeFunc_215526_b for CubePos in SkyLightStorage
        for (int x = 0; x < ICube.CUBE_DIAMETER; x++) {
            for (int z = 0; z < ICube.CUBE_DIAMETER; z++) {
                ((SectionLightStorageAccess) this.storage).invokeFunc_215526_b(new ChunkPos(chunkPos.x + x, chunkPos.z + z).asLong(), p_215620_2_);
            }
        }
    }

}
