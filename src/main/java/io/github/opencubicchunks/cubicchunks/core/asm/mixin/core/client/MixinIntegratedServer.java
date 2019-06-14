package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.client;

import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 256))
    private int getNewBuildLimit(int _256) {
        return Integer.MAX_VALUE/2;
    }
}
