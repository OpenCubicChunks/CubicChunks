package io.github.opencubicchunks.cubicchunks.mixin.core.common.save;

import net.minecraft.world.level.chunk.storage.SectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SectionStorage.class)
public class MixinRegionSectionCache {

    @ModifyConstant(method = "readColumn(Lnet/minecraft/world/level/ChunkPos;Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)V",
            constant = @Constant(intValue = 16))
    public int getMaxSection(int _16) {
        return 32;
    }

}