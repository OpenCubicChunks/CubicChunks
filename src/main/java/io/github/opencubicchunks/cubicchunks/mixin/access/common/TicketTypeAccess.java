package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import java.util.Comparator;

import net.minecraft.server.level.TicketType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TicketType.class)
public interface TicketTypeAccess {
    @Invoker("<init>") static <T> TicketType<T> createNew(String string, Comparator<T> comparator, long l) {
        throw new Error("Mixin did not apply");
    }
}
