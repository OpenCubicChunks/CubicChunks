package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.dimension;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.dimension.ICubicDimension;
import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.extensions.IForgeDimension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Dimension.class)
public abstract class MixinDimension implements IForgeDimension, ICubicDimension {

    private int maxHeight;

    private int minHeight;


    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    public void on$init(World world, DimensionType dimensionType, float p_i225788_3_, CallbackInfo ci) {

        // TODO: check GUI override

        // if the ModDimension is an ICubicDimension, use the heights provided by it.
        if (dimensionType.getModType() instanceof ICubicDimension) {
            ICubicDimension cubicDimensionType = (ICubicDimension) dimensionType.getModType();
            this.maxHeight = cubicDimensionType.getMaxHeight();
            this.minHeight = cubicDimensionType.getMinHeight();
        }

        // TODO: try finding config values

        // TODO: Remove this. We probably shouldn't default to cubic worlds.
        else {
            this.maxHeight = CubicChunks.worldMAXHeight;
            this.minHeight = -CubicChunks.worldMAXHeight;
        }
    }


    @Override
    public int getMaxHeight() {
        return maxHeight;
    }

    @Override
    public int getMinHeight() {
        return minHeight;
    }


    /**
     * Use the more specific getMaxHeight() instead.
     */
    @Override @Deprecated
    public int getHeight() {
        return maxHeight;
    }

    /**
     * Use the more specific getMaxHeight() instead.
     */
    @Override @Deprecated
    public int getActualHeight() {
        return maxHeight;
    }
}
