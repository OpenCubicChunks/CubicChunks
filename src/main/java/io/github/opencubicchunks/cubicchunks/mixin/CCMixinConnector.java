package io.github.opencubicchunks.cubicchunks.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

public class CCMixinConnector implements IMixinConnector {

    @Override public void connect() {
        Mixins.addConfiguration("cubicchunks.mixins.core.json");
        Mixins.addConfiguration("cubicchunks.mixins.access.json");
        Mixins.addConfiguration("cubicchunks.mixins.asm.json");
        Mixins.addConfiguration("cubicchunks.mixins.optifine.json");
        InjectionPoint.register(BeforeInstanceofInjectionPoint.class);
    }
}
