package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import net.minecraftforge.common.extensions.IForgeDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(IForgeDimension.class)
public interface MixinIForgeDimension {
    /**
     * @author Voronoii
     */
    @Overwrite(remap = false)
    default int getHeight()
    {
        return CubicChunks.worldMAXHeight;
    }

}
