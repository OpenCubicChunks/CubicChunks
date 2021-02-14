package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.LightHeightmapGetter;
import io.github.opencubicchunks.cubicchunks.chunk.heightmap.LightSurfaceTrackerWrapper;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ProtoChunk.class)
public abstract class MixinProtoChunk implements LightHeightmapGetter {
    @Shadow
    public abstract ChunkStatus getStatus();

    private LightSurfaceTrackerWrapper lightHeightmap;

    @Override
    public Heightmap getLightHeightmap() {
        return lightHeightmap;
    }

    @Redirect(
        method = "<init>(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;[Lnet/minecraft/world/level/chunk/LevelChunkSection;"
            + "Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/chunk/ProtoTickList;Lnet/minecraft/world/level/LevelHeightAccessor;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelHeightAccessor;getSectionsCount()I"))
    private int getFakeSectionCount(LevelHeightAccessor accessor) {
        return 16; // TODO: properly handle ProtoChunk
    }

    @Inject(
        method = "setStatus(Lnet/minecraft/world/level/chunk/ChunkStatus;)V",
        at = @At("RETURN")
    )
    private void onSetStatus(ChunkStatus status, CallbackInfo ci) {
        // TODO can this run on the client? Will break things if so.
        if (lightHeightmap == null && this.getStatus().isOrAfter(ChunkStatus.FEATURES)) {
            // Lighting only starts happening after FEATURES, so we init here to avoid creating unnecessary heightmaps
            lightHeightmap = new LightSurfaceTrackerWrapper((ChunkAccess) this);
        }
    }
}
