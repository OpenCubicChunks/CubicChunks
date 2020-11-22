package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DistanceManager.PlayerTicketTracker.class)
public interface PlayerTicketTrackerFactoryAccess {

    @Invoker("<init>")
    static DistanceManager.PlayerTicketTracker construct(DistanceManager ticketManager, int p_i50684_2_) {
        throw new Error("Mixin did not apply.");
    }
}