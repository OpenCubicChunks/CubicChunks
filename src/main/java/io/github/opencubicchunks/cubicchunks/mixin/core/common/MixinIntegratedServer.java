package io.github.opencubicchunks.cubicchunks.mixin.core.common;

import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(IntegratedServer.class)
public class MixinIntegratedServer {

    //Sets the new height and removes the warning players recieve when attempting to place blocks beyond 256.
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 256))
    private int setCCHeightLimit(int orig) {
        return 512;
    }
}