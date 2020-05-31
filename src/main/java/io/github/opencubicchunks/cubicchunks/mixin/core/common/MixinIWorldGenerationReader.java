package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.CubicChunks;
import net.minecraft.world.gen.IWorldGenerationBaseReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IWorldGenerationBaseReader.class)
public interface MixinIWorldGenerationReader {

    /**
     * @author Voronoi
     */
    @Overwrite(remap = false)
    default int getMaxHeight() {
        return this instanceof net.minecraft.world.IWorld ?
                ((net.minecraft.world.IWorld)this).getWorld().getDimension().getHeight() : CubicChunks.worldMAXHeight;
    }
}
