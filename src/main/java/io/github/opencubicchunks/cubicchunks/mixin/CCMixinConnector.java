package io.github.opencubicchunks.cubicchunks.mixin;

import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

// TODO: no mixin connector
public class CCMixinConnector implements IMixinConnector {

    @Override public void connect() {
        InjectionPoint.register(BeforeInstanceofInjectionPoint.class);
    }
}