package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import io.github.opencubicchunks.cubicchunks.world.storage.CubeProtoTickList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.chunk.ProtoTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProtoTickList.class)
public class MixinProtoTickList<T> {
    @Shadow private LevelHeightAccessor levelHeightAccessor;

    @Redirect(method = "<init>(Ljava/util/function/Predicate;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/ListTag;Lnet/minecraft/world/level/LevelHeightAccessor;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getSectionsCount()I"))
    private int getFakeSectionCount(LevelHeightAccessor accessor) {
        //This is gross but due to vanilla chunks and cubes being pushed through together in cubic worlds, we have to specifically check for our implementation
        if (!((CubicLevelHeightAccessor) accessor).isCubic() || accessor instanceof CubeProtoTickList.CubeProtoTickListHeightAccess) {
            return accessor.getSectionsCount();
        }

        return 0;
    }

    /**
     * @author Barteks2x
     * @reason TODO tick scheduling, this data needs to be in cubes, but the code for it also in Chunks...
     */
    @Inject(method = "scheduleTick", at = @At("HEAD"), cancellable = true)
    public void scheduleTick(BlockPos blockPos, T object, int i, TickPriority tickPriority, CallbackInfo ci) {
        //This is gross but due to vanilla chunks and cubes being pushed through together in cubic worlds, we have to specifically check for our implementation
        if (!((CubicLevelHeightAccessor) this.levelHeightAccessor).isCubic() || this.levelHeightAccessor instanceof CubeProtoTickList.CubeProtoTickListHeightAccess) {
            return;
        }

        ci.cancel();
        //ChunkAccess.getOrCreateOffsetList(this.toBeTicked, this.levelHeightAccessor.getSectionIndex(blockPos.getY())).add(ProtoChunk
        // .packOffsetCoordinates(blockPos));
    }
}
