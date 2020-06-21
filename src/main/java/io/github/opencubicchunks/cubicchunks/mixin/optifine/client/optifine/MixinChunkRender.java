package io.github.opencubicchunks.cubicchunks.mixin.optifine.client.optifine;

import io.github.opencubicchunks.cubicchunks.chunk.IClientCubeProvider;
import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;
import io.github.opencubicchunks.cubicchunks.chunk.cube.EmptyCube;
import io.github.opencubicchunks.cubicchunks.optifine.IOptiFineChunkRender;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("ShadowTarget")
@Mixin(ChunkRenderDispatcher.ChunkRender.class)
public abstract class MixinChunkRender implements IOptiFineChunkRender {

    @Shadow @Final private BlockPos.Mutable position;
    @Shadow(remap = false) @Final ChunkRenderDispatcher this$0;
    @Shadow(remap = false) public int regionX;
    @Shadow(remap = false) public int regionZ;
    @Dynamic @Shadow(remap = false) private ChunkRenderDispatcher.ChunkRender[] renderChunkNeighboursValid;
    @Dynamic @Shadow(remap = false) private ChunkRenderDispatcher.ChunkRender[] renderChunkNeighbours;

    @Shadow public abstract BlockPos getPosition();

    private BigCube cube;

    @Override public ChunkSection getCube() {
        BigCube cube = this.cube;
        if (cube instanceof EmptyCube) {
            return null;
        }
        if (cube == null || !cube.getLoaded()) {
            cube = (BigCube) ((IClientCubeProvider) ((ChunkRenderDispatcherAccess) this$0).getWorld().getChunkProvider())
                    .getCube(Coords.blockToCube(position.getX()), Coords.blockToCube(position.getY()), Coords.blockToCube(position.getZ()),
                            ChunkStatus.FULL, true);
            assert cube != null;
            this.cube = cube;
            if (cube instanceof EmptyCube) {
                return null;
            }
        }
        return cube.getCubeSections()[Coords.blockToIndex(position.getX(), position.getY(), position.getZ())];
    }

    @Override public int getRegionX() {
        return regionX;
    }

    @Override public int getRegionY() {
        return regionZ;
    }

    @Dynamic @Inject(method = "updateRenderChunkNeighboursValid()V", at = @At("HEAD"), remap = false)
    private void onUpdateNeighbors(CallbackInfo cbi) {
        // if (!isCubic) {
        //     return;
        // }
        int y = this.getPosition().getY();
        int up = Direction.UP.ordinal();
        int down = Direction.DOWN.ordinal();
        this.renderChunkNeighboursValid[up] = this.renderChunkNeighbours[up].getPosition().getY() == y + 16 ?
                this.renderChunkNeighbours[up] : null;
        this.renderChunkNeighboursValid[down] = this.renderChunkNeighbours[down].getPosition().getY() == y - 16 ?
                this.renderChunkNeighbours[down] : null;
    }
}
