package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ProtoChunk.class)
public class MixinProtoChunk {
    @Redirect(method = "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;[Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/LevelHeightAccessor;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getSectionsCount()I"))
    private int getFakeSectionCount(LevelHeightAccessor accessor) {
        return 16; // TODO: properly handle ProtoChunk
    }
}
