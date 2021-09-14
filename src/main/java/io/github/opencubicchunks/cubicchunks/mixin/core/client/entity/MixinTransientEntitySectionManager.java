package io.github.opencubicchunks.cubicchunks.mixin.core.client.entity;

import io.github.opencubicchunks.cubicchunks.chunk.entity.IsCubicEntityContext;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(TransientEntitySectionManager.class)
public class MixinTransientEntitySectionManager<T extends EntityAccess> implements IsCubicEntityContext {


    @Shadow @Final private EntitySectionStorage<T> sectionStorage;

    private boolean isCubic;

    @Override public boolean isCubic() {
        return this.isCubic;
    }

    @Override public void setIsCubic(boolean isCubic) {
        this.isCubic = isCubic;
        ((IsCubicEntityContext) this.sectionStorage).setIsCubic(isCubic);
    }
}
