package cubicchunks.cc.mixin;

import org.spongepowered.asm.mixin.connect.IMixinConnector;
import org.spongepowered.asm.mixin.injection.InjectionPoint;

public class CCMixinConnector implements IMixinConnector {

    @Override public void connect() {
        InjectionPoint.register(BeforeInstanceofInjectionPoint.class);
    }
}
