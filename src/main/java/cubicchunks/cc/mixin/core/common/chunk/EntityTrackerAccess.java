package cubicchunks.cc.mixin.core.common.chunk;

import net.minecraft.entity.Entity;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkManager.EntityTracker.class)
public interface EntityTrackerAccess {
    @Accessor Entity getEntity();
}
