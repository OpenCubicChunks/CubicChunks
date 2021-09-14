package io.github.opencubicchunks.cubicchunks.mixin.core.common.level;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.world.level.CubicLevelHeightAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldGenRegion.class)
public class MixinWorldGenRegion implements CubicLevelHeightAccessor {

    private boolean isCubic;
    private boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setCubic(ServerLevel serverLevel, List<ChunkAccess> list, ChunkStatus status, int i, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) serverLevel).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) serverLevel).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) serverLevel).worldStyle();
    }

    @Override public WorldStyle worldStyle() {
        return worldStyle;
    }

    @Override public boolean isCubic() {
        return isCubic;
    }

    @Override public boolean generates2DChunks() {
        return generates2DChunks;
    }
}
