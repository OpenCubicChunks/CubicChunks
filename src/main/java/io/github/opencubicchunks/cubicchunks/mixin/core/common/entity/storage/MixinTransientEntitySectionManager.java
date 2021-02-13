package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(TransientEntitySectionManager.class)
public class MixinTransientEntitySectionManager<T extends EntityAccess> {


}