package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IBlockReader.class)
public interface IBlockReaderMixin {
    /**
     * @author Voronoi
     * @reason Need to overwrite as this is an interface.
     */
    @Overwrite
    default int getHeight() {
        return CubicChunks.MAX_SUPPORTED_HEIGHT;
    }

    default int minHeight() { return CubicChunks.MIN_SUPPORTED_HEIGHT; }
    default int maxHeight() { return this.getHeight(); }
}
