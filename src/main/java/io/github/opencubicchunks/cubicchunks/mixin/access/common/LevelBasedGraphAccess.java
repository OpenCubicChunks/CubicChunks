package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.lighting.LevelBasedGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelBasedGraph.class)
public interface LevelBasedGraphAccess {
    @Invoker("scheduleUpdate") void invokeScheduleUpdate(long fromPos, long toPos, int newLevel, boolean isDecreasing);
    @Invoker("getEdgeLevel") int invokeGetEdgeLevel(long startPos, long endPos, int startLevel);
    @Invoker("getLevel") int invokeGetLevel(long sectionPosIn);
}
