package io.github.opencubicchunks.cubicchunks.mixin.core.common.ticket.access;

import net.minecraft.world.lighting.LevelBasedGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelBasedGraph.class)
public interface LevelBasedGraphAccess {
    @Invoker("scheduleUpdate") void scheduleMixedUpdate(long fromPos, long toPos, int newLevel, boolean isDecreasing);
}
