package io.github.opencubicchunks.cubicchunks.mixin.access.client;

import net.minecraft.client.Option;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Option.class)
public interface OptionAccess {

    @Invoker Component invokeGenericValueLabel(Component value);
}
