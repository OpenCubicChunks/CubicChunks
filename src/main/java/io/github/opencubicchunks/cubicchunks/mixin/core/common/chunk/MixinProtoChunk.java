package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProtoChunk.class)
public abstract class MixinProtoChunk implements LevelHeightAccessor {
    @Shadow public abstract ChunkStatus getStatus();

    @Shadow @Final private LevelHeightAccessor levelHeightAccessor;

    @Redirect(
        method = "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;[Lnet/minecraft/world/level/chunk/LevelChunkSection;"
            + "Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/LevelHeightAccessor;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getSectionsCount()I"))
    private int getFakeSectionCount(LevelHeightAccessor accessor) {
        if (accessor instanceof Level && CubicChunks.COLUMN_DIMENSION_MAP.contains(((Level) accessor).dimension().location().toString())) {
            return 24;
        }

        return Math.min(IBigCube.SECTION_COUNT * 2, accessor.getSectionsCount()); // TODO: properly handle ProtoChunk
    }

    @Inject(method = "getHeight()I", at = @At("HEAD"), cancellable = true)
    private void setHeight(CallbackInfoReturnable<Integer> cir) {
        if (this.levelHeightAccessor instanceof Level && CubicChunks.COLUMN_DIMENSION_MAP.contains(((Level) this.levelHeightAccessor).dimension().location().toString())) {
            cir.setReturnValue(384);
        }
    }

    @Inject(method = "getMinBuildHeight", at = @At("HEAD"), cancellable = true)
    private void setMinheight(CallbackInfoReturnable<Integer> cir) {
        if (this.levelHeightAccessor instanceof Level && CubicChunks.COLUMN_DIMENSION_MAP.contains(((Level) this.levelHeightAccessor).dimension().location().toString())) {
            cir.setReturnValue(-64);
        }
    }
}
