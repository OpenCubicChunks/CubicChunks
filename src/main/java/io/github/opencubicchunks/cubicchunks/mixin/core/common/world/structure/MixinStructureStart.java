package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.structure;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import io.github.opencubicchunks.cubicchunks.world.ICubicStructureStart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(StructureStart.class)
public abstract class MixinStructureStart implements ICubicStructureStart {

    private int sectionY;
    private boolean has3dPlacement;

    @Shadow protected abstract BoundingBox createBoundingBox();

    @Override public void init3dPlacement(int sectionY) {
        this.sectionY = sectionY;
        this.has3dPlacement = true;
    }

    @Override public boolean has3DPlacement() {
        return this.has3dPlacement;
    }

    @ModifyArg(method = "getLocatePos", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;<init>(III)V"), index = 1)
    private int getStructureY(int arg0) {
        return (this.createBoundingBox().minY() + this.createBoundingBox().maxY()) >> 1;
    }
}
