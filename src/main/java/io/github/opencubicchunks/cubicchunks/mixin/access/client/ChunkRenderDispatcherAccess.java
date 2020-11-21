package io.github.opencubicchunks.cubicchunks.mixin.access.client;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkRenderDispatcher.class)
public interface ChunkRenderDispatcherAccess {

    @Accessor Level getLevel();

}
