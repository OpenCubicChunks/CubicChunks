package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.TickPriority;
import net.minecraft.world.level.chunk.ProtoTickList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProtoTickList.class)
public class MixinProtoTickList<T> {
    @Redirect(method = "<init>(Ljava/util/function/Predicate;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/nbt/ListTag;Lnet/minecraft/world/level/LevelHeightAccessor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getSectionsCount()I"))
    private int getFakeSectionCount(LevelHeightAccessor accessor) {
        return 0;
    }

    /**
     * @author Barteks2x
     * @reason TODO tick scheduling, this data needs to be in cubes, but the code for it also in Chunks...
     */
    @Overwrite
    public void scheduleTick(BlockPos blockPos, T object, int i, TickPriority tickPriority) {
        //ChunkAccess.getOrCreateOffsetList(this.toBeTicked, this.levelHeightAccessor.getSectionIndex(blockPos.getY())).add(ProtoChunk
        // .packOffsetCoordinates(blockPos));
    }
}
