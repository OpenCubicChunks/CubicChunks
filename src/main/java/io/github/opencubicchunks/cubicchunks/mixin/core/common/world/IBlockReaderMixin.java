package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(BlockGetter.class)
public interface IBlockReaderMixin {
    /**
     * @author Voronoi
     * @reason Need to overwrite as this is an interface.
     */
    @Overwrite
    default int getMaxBuildHeight() {
        return CubicChunks.MAX_SUPPORTED_HEIGHT;
    }

    default int minHeight() { return CubicChunks.MIN_SUPPORTED_HEIGHT; }
    default int maxHeight() { return this.getMaxBuildHeight(); }
}