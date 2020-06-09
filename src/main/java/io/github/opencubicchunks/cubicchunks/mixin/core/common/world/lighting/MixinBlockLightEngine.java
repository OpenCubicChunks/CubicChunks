package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.world.lighting.ICubeLightProvider;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.lighting.BlockLightEngine;
import net.minecraft.world.lighting.BlockLightStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockLightEngine.class)
public class MixinBlockLightEngine extends MixinLightEngine<BlockLightStorage.StorageMap, BlockLightStorage> {

    /**
     * @author NotStirred
     * @reason Vanilla lighting is bye bye
     */
    @Overwrite
    private int getLightValue(long worldPos) {
        int blockX = BlockPos.unpackX(worldPos);
        int blockY = BlockPos.unpackY(worldPos);
        int blockZ = BlockPos.unpackZ(worldPos);
        IBlockReader iblockreader = ((ICubeLightProvider)this.chunkProvider).getCubeForLight(
                SectionPos.toChunk(blockX),
                SectionPos.toChunk(blockY),
                SectionPos.toChunk(blockZ)
        );
        return iblockreader != null ? iblockreader.getLightValue(this.scratchPos.setPos(blockX, blockY, blockZ)) : 0;
    }

}
