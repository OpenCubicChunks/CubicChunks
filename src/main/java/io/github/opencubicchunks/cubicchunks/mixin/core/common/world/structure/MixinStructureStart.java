package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import io.github.opencubicchunks.cubicchunks.world.ICubicStructureStart;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements ICubicStructureStart {
    private boolean has3dPlacement;

    @Shadow protected abstract BoundingBox createBoundingBox();

    @Override public void init3dPlacement() {
        this.has3dPlacement = true;
    }

    @Override public boolean has3DPlacement() {
        return this.has3dPlacement;
    }

    @ModifyArg(method = "getLocatePos", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;<init>(III)V"), index = 1)
    private int getStructureY(int arg0) {
        if (this.has3dPlacement) {
            return (this.createBoundingBox().minY() + this.createBoundingBox().maxY()) >> 1;
        }
        return arg0;
    }
}
