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

    @Shadow @Final protected BlockPos.Mutable scratchPos;

    @Shadow @Final protected IChunkLightProvider chunkProvider;

    @Shadow @Final private long[] recentPositions;

    @Shadow @Final private IBlockReader[] recentChunks;

    @Override
    public void retainCubeData(CubePos pos, boolean retain) {
        long i = pos.asSectionPos().asLong();
        ((ISectionLightStorage)this.storage).retainCubeData(i, retain);
    }

    @Override
    public void func_215620_a(CubePos cubePos, boolean enable) {
        ChunkPos chunkPos = cubePos.asChunkPos();
        //TODO: implement invokeFunc_215526_b for CubePos in SkyLightStorage
        for (int x = 0; x < IBigCube.DIAMETER_IN_SECTIONS; x++) {
            for (int z = 0; z < IBigCube.DIAMETER_IN_SECTIONS; z++) {
                ((SectionLightStorageAccess) this.storage).invokeSetColumnEnabled(new ChunkPos(chunkPos.x + x, chunkPos.z + z).asLong(), enable);
            }
        }
    }

    /**
     * @author NotStirred
     * @reason Vanilla lighting is gone
     */
    //TODO: make this into a redirect that calls getCubeReader taking arguments blockPosLong
    @Overwrite // getBlockAndOpacity
    protected BlockState getBlockAndOpacity(long blockPosLong, @Nullable MutableInt opacity) {
        if (blockPosLong == Long.MAX_VALUE) {
            if (opacity != null) {
                opacity.setValue(0);
            }

            return Blocks.AIR.getDefaultState();
        } else {
            int sectionX = SectionPos.toChunk(BlockPos.unpackX(blockPosLong));
            int sectionY = SectionPos.toChunk(BlockPos.unpackY(blockPosLong));
            int sectionZ = SectionPos.toChunk(BlockPos.unpackZ(blockPosLong));
            IBlockReader iblockreader = this.getCubeReader(sectionX, sectionY, sectionZ);
            if (iblockreader == null) {
                if (opacity != null) {
                    opacity.setValue(16);
                }

                return Blocks.BEDROCK.getDefaultState();
            } else {
                this.scratchPos.setPos(blockPosLong);
                BlockState blockstate = iblockreader.getBlockState(this.scratchPos);
                boolean flag = blockstate.isSolid() && blockstate.isTransparent();
                if (opacity != null) {
                    opacity.setValue(blockstate.getOpacity(this.chunkProvider.getWorld(), this.scratchPos));
                }

                return flag ? blockstate : Blocks.AIR.getDefaultState();
            }
        }
    }

    @Nullable
    private IBlockReader getCubeReader(int sectionX, int sectionY, int sectionZ) {
        long i = SectionPos.asLong(sectionX, sectionY, sectionZ);

        for(int j = 0; j < 2; ++j) {
            if (i == this.recentPositions[j]) {
                return this.recentChunks[j];
            }
        }

        IBlockReader iblockreader = ((ICubeLightProvider)this.chunkProvider).getCubeForLight(sectionX, sectionY, sectionZ);

        for(int k = 1; k > 0; --k) {
            this.recentPositions[k] = this.recentPositions[k - 1];
            this.recentChunks[k] = this.recentChunks[k - 1];
        }

        this.recentPositions[0] = i;
        this.recentChunks[0] = iblockreader;
        return iblockreader;
    }
}
