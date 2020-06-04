package io.github.opencubicchunks.cubicchunks.mixin.asm.common;

import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({
        ChunkManager.ProxyTicketManager.class,
        ChunkManager.class
})
public class MixinAsmTarget {
    // intentionally empty
}
