package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting;

import io.github.opencubicchunks.cubicchunks.world.lighting.ICubeLightProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.lighting.BlockLightEngine;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockLightEngine.class)
public class MixinBlockLightEngine extends MixinLightEngine<BlockLightSectionStorage.BlockDataLayerStorageMap, BlockLightSectionStorage> {

    /**
     * @author NotStirred
     * @reason Vanilla lighting is bye bye
     */
    @Overwrite
    private int getLightEmission(long worldPos) {
        int blockX = BlockPos.getX(worldPos);
        int blockY = BlockPos.getY(worldPos);
        int blockZ = BlockPos.getZ(worldPos);
        BlockGetter iblockreader = ((ICubeLightProvider)this.chunkSource).getCubeForLighting(
                SectionPos.blockToSectionCoord(blockX),
                SectionPos.blockToSectionCoord(blockY),
                SectionPos.blockToSectionCoord(blockZ)
        );
        return iblockreader != null ? iblockreader.getLightEmission(this.pos.set(blockX, blockY, blockZ)) : 0;
    }

}