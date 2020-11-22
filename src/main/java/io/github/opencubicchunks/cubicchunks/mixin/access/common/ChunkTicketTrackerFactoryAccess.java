package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.server.level.DistanceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DistanceManager.ChunkTicketTracker.class)
public interface ChunkTicketTrackerFactoryAccess {

    @Invoker("<init>")
    static DistanceManager.ChunkTicketTracker construct(DistanceManager ticketManager) {
        throw new Error("Mixin did not apply.");
    }
}