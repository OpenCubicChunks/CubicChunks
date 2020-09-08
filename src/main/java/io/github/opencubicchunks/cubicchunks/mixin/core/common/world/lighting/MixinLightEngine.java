package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.access.common.SectionLightStorageAccess;
import io.github.opencubicchunks.cubicchunks.world.lighting.ICubeLightProvider;
import io.github.opencubicchunks.cubicchunks.world.lighting.ILightEngine;
import io.github.opencubicchunks.cubicchunks.world.lighting.ISectionLightStorage;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.lighting.LightDataMap;
import net.minecraft.world.lighting.LightEngine;
import net.minecraft.world.lighting.SectionLightStorage;
import org.apache.commons.lang3.mutable.MutableInt;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(LightEngine.class)
public class MixinLightEngine <M extends LightDataMap<M>, S extends SectionLightStorage<M>> implements ILightEngine {

    @Shadow @Final protected S storage;

    @Shadow @Final protected BlockPos.Mutable pos;

    @Shadow @Final protected IChunkLightProvider chunkSource;

    @Shadow @Final private long[] lastChunkPos;

    @Shadow @Final private IBlockReader[] lastChunk;

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
            IBlockReader iblockreader = this.getCubeReader(sectionX, sectionY, sectionZ);
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
    private IBlockReader getCubeReader(int sectionX, int sectionY, int sectionZ) {
        long i = SectionPos.asLong(sectionX, sectionY, sectionZ);

        for(int j = 0; j < 2; ++j) {
            if (i == this.lastChunkPos[j]) {
                return this.lastChunk[j];
            }
        }

        IBlockReader iblockreader = ((ICubeLightProvider)this.chunkSource).getCubeForLighting(sectionX, sectionY, sectionZ);

        for(int k = 1; k > 0; --k) {
            this.lastChunkPos[k] = this.lastChunkPos[k - 1];
            this.lastChunk[k] = this.lastChunk[k - 1];
        }

        this.lastChunkPos[0] = i;
        this.lastChunk[0] = iblockreader;
        return iblockreader;
    }
}