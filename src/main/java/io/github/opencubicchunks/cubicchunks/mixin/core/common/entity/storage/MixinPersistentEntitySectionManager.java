package io.github.opencubicchunks.cubicchunks.mixin.core.common.entity.storage;

import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PersistentEntitySectionManager.class)
public class MixinPersistentEntitySectionManager<T extends EntityAccess> {



}