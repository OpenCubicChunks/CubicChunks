package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.mixin.core.common.world.dimension.MixinDimension;
import io.github.opencubicchunks.cubicchunks.world.dimension.ICubicDimension;
import net.minecraft.world.dimension.Dimension;
import net.minecraftforge.common.extensions.IForgeDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IForgeDimension.class)
public interface MixinIForgeDimension {


    @Shadow Dimension getDimension();

    /**
     * @author Voronoii, Cyclonit
     */
    @Overwrite(remap = false)
    default int getHeight()
    {
        return ((ICubicDimension) getDimension()).getMaxHeight();
    }

}
