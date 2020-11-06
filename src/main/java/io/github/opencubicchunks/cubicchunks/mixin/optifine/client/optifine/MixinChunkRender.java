package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.optifine;

import io.github.opencubicchunks.cubicchunks.chunk.IClientCubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.optifine.IOptiFineChunkRender;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("ShadowTarget")
@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public abstract class MixinChunkRender implements IOptiFineChunkRender {

    @Shadow @Final private BlockPos.MutableBlockPos origin;
    @SuppressWarnings("target") @Shadow(aliases = "this$0", remap = false) @Final ChunkRenderDispatcher syntheticThis;
    @SuppressWarnings("target") @Shadow(remap = false) public int regionX;
    @SuppressWarnings("target") @Shadow(remap = false) public int regionZ;
    @SuppressWarnings("target") @Dynamic @Shadow(remap = false) private ChunkRenderDispatcher.RenderChunk[] renderChunkNeighboursValid;
    @SuppressWarnings("target") @Dynamic @Shadow(remap = false) private ChunkRenderDispatcher.RenderChunk[] renderChunkNeighbours;

    @Shadow public abstract BlockPos getOrigin();

    private BigCube cube;

    @Override public LevelChunkSection getCube() {
        BigCube cube = this.cube;
        if (cube instanceof EmptyCube) {
            return null;
        }
        if (cube == null || !cube.getLoaded()) {
            cube = (BigCube) ((IClientCubeProvider) ((ChunkRenderDispatcherAccess) syntheticThis).getLevel().getChunkSource())
                    .getCube(Coords.blockToCube(origin.getX()), Coords.blockToCube(origin.getY()), Coords.blockToCube(origin.getZ()),
                            ChunkStatus.FULL, true);
            assert cube != null;
            this.cube = cube;
            if (cube instanceof EmptyCube) {
                return null;
            }
        }
        return cube.getCubeSections()[Coords.blockToIndex(origin.getX(), origin.getY(), origin.getZ())];
    }

    @Override public int getRegionX() {
        return regionX;
    }

    @Override public int getRegionY() {
        return regionZ;
    }

    @SuppressWarnings("target")
    @Dynamic @Inject(method = "updateRenderChunkNeighboursValid()V", at = @At("HEAD"), remap = false)
    private void onUpdateNeighbors(CallbackInfo cbi) {
        // if (!isCubic) {
        //     return;
        // }
        int y = this.getOrigin().getY();
        int up = Direction.UP.ordinal();
        int down = Direction.DOWN.ordinal();
        this.renderChunkNeighboursValid[up] = this.renderChunkNeighbours[up].getOrigin().getY() == y + 16 ?
                this.renderChunkNeighbours[up] : null;
        this.renderChunkNeighboursValid[down] = this.renderChunkNeighbours[down].getOrigin().getY() == y - 16 ?
                this.renderChunkNeighbours[down] : null;
    }
}