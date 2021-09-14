package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.world.level.lighting.DynamicGraphMinFixedPoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DynamicGraphMinFixedPoint.class)
public interface DynamicGraphMinFixedPointAccess {
    @Invoker void invokeCheckEdge(long fromPos, long toPos, int newLevel, boolean isDecreasing);
    @Invoker int invokeComputeLevelFromNeighbor(long startPos, long endPos, int startLevel);
    @Invoker int invokeGetLevel(long sectionPosIn);
}