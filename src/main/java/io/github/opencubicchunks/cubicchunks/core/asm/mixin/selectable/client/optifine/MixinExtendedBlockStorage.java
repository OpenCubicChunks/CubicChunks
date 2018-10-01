package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineExtendedBlockStorage;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ExtendedBlockStorage.class)
public abstract class MixinExtendedBlockStorage implements IOptifineExtendedBlockStorage {
    // method implemented by OptiFine
}
