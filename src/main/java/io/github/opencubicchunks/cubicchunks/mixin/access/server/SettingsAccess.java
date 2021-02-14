package io.github.opencubicchunks.cubicchunks.mixin.access.server;

import java.util.function.Function;

import net.minecraft.server.dedicated.Settings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Settings.class)
public interface SettingsAccess {


    @Invoker int invokeGet(String key, int fallback);
}
