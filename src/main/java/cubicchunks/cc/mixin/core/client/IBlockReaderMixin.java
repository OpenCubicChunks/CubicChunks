package cubicchunks.cc.mixin.core.client;

import cubicchunks.cc.CubicChunks;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IBlockReader.class)
public interface IBlockReaderMixin {
    /**
     * @author Voronoi
     */
    @Overwrite
    default int getHeight() {
        return CubicChunks.worldMAXHeight;
    }
}
