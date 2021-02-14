package io.github.opencubicchunks.cubicchunks.mixin.core.server;

import java.util.Properties;

import io.github.opencubicchunks.cubicchunks.mixin.access.server.SettingsAccess;
import io.github.opencubicchunks.cubicchunks.server.IDedicatedServerProperties;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DedicatedServerProperties.class)
public class MixinDedicatedServerProperties implements IDedicatedServerProperties {

    private int verticalViewDistance;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addVerticalViewDistanceServerProperty(Properties properties, RegistryAccess registryAccess, CallbackInfo ci) {
        this.verticalViewDistance = ((SettingsAccess)((DedicatedServerProperties)(Object) this)).invokeGet("vertical-view-distance", 10);
    }

    @Override public int getVerticalViewDistance() {
        return this.verticalViewDistance;
    }
}
