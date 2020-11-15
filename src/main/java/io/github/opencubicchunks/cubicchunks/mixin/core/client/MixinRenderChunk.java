package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import io.github.opencubicchunks.cubicchunks.mixin.access.client.ChunkRenderDispatcherAccess;
import io.github.opencubicchunks.cubicchunks.server.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class MixinRenderChunk {

    @Shadow protected abstract double getDistToPlayerSqr();

    @Shadow protected abstract boolean doesChunkExistAt(BlockPos blockPos);

    @Shadow @Final private BlockPos.MutableBlockPos[] relativeOrigins;

    @SuppressWarnings("target") @Shadow(aliases = "field_20833", remap = false) ChunkRenderDispatcher this$0;

    /**
     * Add checks for the cube at that pos
     */
    @Inject(method = "doesChunkExistAt", at = @At("HEAD"), cancellable = true)
    private void doesChunkAndCubeExistAt(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(
            ((ChunkRenderDispatcherAccess) this$0).getLevel().getChunk(
                Coords.blockToSection(blockPos.getX()),
                Coords.blockToSection(blockPos.getZ()),
                ChunkStatus.FULL,
                false) != null &&
            ((ICubicWorld) ((ChunkRenderDispatcherAccess) this$0).getLevel()).getCube(
                Coords.blockToCube(blockPos.getX()),
                Coords.blockToCube(blockPos.getY()),
                Coords.blockToCube(blockPos.getZ()),
                ChunkStatus.FULL,
                false) != null
        );
    }

    /**
     * Add checks for above and below neighbors
     */
    @Inject(method = "hasAllNeighbors", at = @At("HEAD"), cancellable = true)
    private void onHasAllNeighbors(CallbackInfoReturnable<Boolean> cir) {
        if (!(this.getDistToPlayerSqr() > 576.0D)) {
            cir.setReturnValue(true);
        } else {
            cir.setReturnValue(
                this.doesChunkExistAt(this.relativeOrigins[Direction.WEST.ordinal()])
                && this.doesChunkExistAt(this.relativeOrigins[Direction.NORTH.ordinal()])
                && this.doesChunkExistAt(this.relativeOrigins[Direction.EAST.ordinal()])
                && this.doesChunkExistAt(this.relativeOrigins[Direction.SOUTH.ordinal()])
                && this.doesChunkExistAt(this.relativeOrigins[Direction.UP.ordinal()])
                && this.doesChunkExistAt(this.relativeOrigins[Direction.DOWN.ordinal()])
            );
        }
    }

}