package io.github.opencubicchunks.cubicchunks.mixin.access.common;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkMap.TrackedEntity.class)
public interface EntityTrackerAccess {
    @Accessor Entity getEntity();
}