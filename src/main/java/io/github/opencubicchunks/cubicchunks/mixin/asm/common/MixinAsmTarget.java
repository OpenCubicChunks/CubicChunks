package io.github.opencubicchunks.cubicchunks.mixin.asm.common;

import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
        ChunkManager.ProxyTicketManager.class,
        ChunkManager.class,
        ChunkHolder.class
})
public class MixinAsmTarget {
    // intentionally empty
}
