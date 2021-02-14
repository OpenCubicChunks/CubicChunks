package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.client.IVerticalViewDistance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Options.class)
public class MixinGameOptionsScreen implements IVerticalViewDistance {

    private int verticalViewDistance;


    @Override public void setVerticalViewDistance(int viewDistance) {
        this.verticalViewDistance = viewDistance;
    }

    @Override public int getVerticalViewDistance() {
        return verticalViewDistance;
    }
}
