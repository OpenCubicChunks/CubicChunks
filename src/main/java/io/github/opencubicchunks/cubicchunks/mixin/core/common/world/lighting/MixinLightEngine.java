package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.SectionLightStorageAccess;
import io.github.opencubicchunks.cubicchunks.world.lighting.ICubeLightProvider;
import io.github.opencubicchunks.cubicchunks.world.lighting.ILightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.ISectionLightStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightEngine;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(LayerLightEngine.class)
public class MixinLightEngine <M extends DataLayerStorageMap<M>, S extends LayerLightSectionStorage<M>> implements ILightEngine {

    @Shadow @Final protected S storage;

    @Shadow @Final protected BlockPos.MutableBlockPos pos;

    @Shadow @Final protected LightChunkGetter chunkSource;

    @Shadow @Final private long[] lastChunkPos;

    @Shadow @Final private BlockGetter[] lastChunk;

    @Override
    public void retainCubeData(CubePos pos, boolean retain) {
        long i = pos.asSectionPos().asLong();
        ((ISectionLightStorage)this.storage).retainCubeData(i, retain);
    }

    @Override
    public void enableLightSources(CubePos cubePos, boolean enable) {
        ChunkPos chunkPos = cubePos.asChunkPos();
        //TODO: implement invokeEnableLightSources for CubePos in SkyLightStorage
        for (int x = 0; x < IBigCube.DIAMETER_IN_SECTIONS; x++) {
            for (int z = 0; z < IBigCube.DIAMETER_IN_SECTIONS; z++) {
                ((SectionLightStorageAccess) this.storage).invokeSetColumnEnabled(new ChunkPos(chunkPos.x + x, chunkPos.z + z).toLong(), enable);
            }
        }
    }

    /**
     * @author NotStirred
     * @reason Vanilla lighting is gone
     */
    //TODO: make this into a redirect that calls getCubeReader taking arguments blockPosLong
    @Overwrite
    protected BlockState getStateAndOpacity(long blockPosLong, @Nullable MutableInt opacity) {
        if (blockPosLong == Long.MAX_VALUE) {
            if (opacity != null) {
                opacity.setValue(0);
            }

            return Blocks.AIR.defaultBlockState();
        } else {
            int sectionX = SectionPos.blockToSectionCoord(BlockPos.getX(blockPosLong));
            int sectionY = SectionPos.blockToSectionCoord(BlockPos.getY(blockPosLong));
            int sectionZ = SectionPos.blockToSectionCoord(BlockPos.getZ(blockPosLong));
            BlockGetter iblockreader = this.getCubeReader(sectionX, sectionY, sectionZ);
            if (iblockreader == null) {
                if (opacity != null) {
                    opacity.setValue(16);
                }

                return Blocks.BEDROCK.defaultBlockState();
            } else {
                this.pos.set(blockPosLong);
                BlockState blockstate = iblockreader.getBlockState(this.pos);
                boolean flag = blockstate.canOcclude() && blockstate.useShapeForLightOcclusion();
                if (opacity != null) {
                    opacity.setValue(blockstate.getLightBlock(this.chunkSource.getLevel(), this.pos));
                }

                return flag ? blockstate : Blocks.AIR.defaultBlockState();
            }
        }
    }

    @Nullable
    private BlockGetter getCubeReader(int sectionX, int sectionY, int sectionZ) {
        long i = SectionPos.asLong(sectionX, sectionY, sectionZ);

        for(int j = 0; j < 2; ++j) {
            if (i == this.lastChunkPos[j]) {
                return this.lastChunk[j];
            }
        }

        BlockGetter iblockreader = ((ICubeLightProvider)this.chunkSource).getCubeForLighting(sectionX, sectionY, sectionZ);

        for(int k = 1; k > 0; --k) {
            this.lastChunkPos[k] = this.lastChunkPos[k - 1];
            this.lastChunk[k] = this.lastChunk[k - 1];
        }

        this.lastChunkPos[0] = i;
        this.lastChunk[0] = iblockreader;
        return iblockreader;
    }
}