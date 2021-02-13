package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import java.util.List;

import io.github.opencubicchunks.cubicchunks.server.CubicLevelHeightAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldGenRegion.class)
public class MixinWorldGenRegion implements CubicLevelHeightAccessor {

    private Boolean isCubic;
    private Boolean generates2DChunks;
    private WorldStyle worldStyle;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void setCubic(ServerLevel serverLevel, List<ChunkAccess> list, CallbackInfo ci) {
        isCubic = ((CubicLevelHeightAccessor) serverLevel).isCubic();
        generates2DChunks = ((CubicLevelHeightAccessor) serverLevel).generates2DChunks();
        worldStyle = ((CubicLevelHeightAccessor) serverLevel).worldStyle();
    }

    @Override public WorldStyle worldStyle() {
                if (worldStyle == null)
            new Error().printStackTrace();
        return worldStyle;
    }

    @Override public Boolean isCubic() {
                if (isCubic == null)
            new Error().printStackTrace();
        return isCubic;
    }

    @Override public Boolean generates2DChunks() {
                if (generates2DChunks == null)
            new Error().printStackTrace();
        return generates2DChunks;
    }
}
