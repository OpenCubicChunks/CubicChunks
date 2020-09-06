package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.optifine;

import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkRenderDispatcher.class)
public interface ChunkRenderDispatcherAccess {

    @Accessor World getWorld();
}