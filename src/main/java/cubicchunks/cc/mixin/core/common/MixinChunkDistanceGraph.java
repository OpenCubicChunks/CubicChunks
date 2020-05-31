package cubicchunks.cc.mixin.core.common;

import cubicchunks.cc.mixin.core.common.ticket.access.LevelBasedGraphAccess;
import net.minecraft.world.chunk.ChunkDistanceGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkDistanceGraph.class)
public class MixinChunkDistanceGraph {

    @Inject(method = "updateSourceLevel(JIZ)V", at = @At("HEAD"))
    private void removeSentinelInSourceLevel(long pos, int level, boolean isDecreasing, CallbackInfo ci) {
        ((LevelBasedGraphAccess)this).scheduleMixedUpdate(Long.MAX_VALUE, pos, level, isDecreasing);

    }
}
