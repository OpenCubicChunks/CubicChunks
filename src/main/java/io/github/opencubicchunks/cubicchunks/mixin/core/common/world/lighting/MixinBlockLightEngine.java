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
    private int getLightEmission(long worldPos) {
        int blockX = BlockPos.getX(worldPos);
        int blockY = BlockPos.getY(worldPos);
        int blockZ = BlockPos.getZ(worldPos);
        IBlockReader iblockreader = ((ICubeLightProvider)this.chunkSource).getCubeForLighting(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ)
        );
        return iblockreader != null ? iblockreader.getLightEmission(this.pos.set(blockX, blockY, blockZ)) : 0;
    }

}