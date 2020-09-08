package io.github.opencubicchunks.cubicchunks.mixin.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ViewFrustum.class)
public abstract class MixinViewFrustum {
    @Shadow public ChunkRenderDispatcher.ChunkRender[] chunks;
    @Shadow protected int chunkGridSizeY;
    @Shadow protected int chunkGridSizeX;
    @Shadow protected int chunkGridSizeZ;

    @Shadow protected abstract int getChunkIndex(int x, int y, int z);

    /**
     * @author Barteks2x
     * @reason vertical view distance = horizontal
     */
    @Overwrite
    protected void setViewDistance(int renderDistanceChunks) {
        int d = renderDistanceChunks * 2 + 1;
        this.chunkGridSizeX = d;
        this.chunkGridSizeY = d;
        this.chunkGridSizeZ = d;
    }

    @Inject(method = "repositionCamera", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void repositionCamera(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        double x = view.getX();
        double y = view.getY();
        double z = view.getZ();
        int viewX = MathHelper.floor(x);
        int viewZ = MathHelper.floor(z);
        int viewY = MathHelper.floor(y);

        for (int xIndex = 0; xIndex < this.chunkGridSizeX; ++xIndex) {
            int xBase = this.chunkGridSizeX * 16;
            int xTemp = viewX - 8 - xBase / 2;
            int posX = xTemp + Math.floorMod(xIndex * 16 - xTemp, xBase);

            for (int zIndex = 0; zIndex < this.chunkGridSizeZ; ++zIndex) {
                int zBase = this.chunkGridSizeZ * 16;
                int zTemp = viewZ - 8 - zBase / 2;
                int posZ = zTemp + Math.floorMod(zIndex * 16 - zTemp, zBase);

                for (int yIndex = 0; yIndex < this.chunkGridSizeY; ++yIndex) {
                    int yBase = this.chunkGridSizeY * 16;
                    int yTemp = viewY - 8 - yBase / 2;
                    int posY = yTemp + Math.floorMod(yIndex * 16 - yTemp, yBase);
                    ChunkRenderDispatcher.ChunkRender chunkrenderdispatcher$chunkrender = this.chunks[this.getChunkIndex(xIndex, yIndex, zIndex)];
                    chunkrenderdispatcher$chunkrender.setOrigin(posX, posY, posZ);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "getRenderChunkAt", at = @At(value = "HEAD"), cancellable = true)
    private void getRenderChunkAt(BlockPos pos, CallbackInfoReturnable<ChunkRenderDispatcher.ChunkRender> cbi) {
        int x = MathHelper.intFloorDiv(pos.getX(), 16);
        int y = MathHelper.intFloorDiv(pos.getY(), 16);
        int z = MathHelper.intFloorDiv(pos.getZ(), 16);
        x = MathHelper.positiveModulo(x, this.chunkGridSizeX);
        y = MathHelper.positiveModulo(y, this.chunkGridSizeY);
        z = MathHelper.positiveModulo(z, this.chunkGridSizeZ);
        ChunkRenderDispatcher.ChunkRender renderChunk = this.chunks[this.getChunkIndex(x, y, z)];
        cbi.cancel();
        cbi.setReturnValue(renderChunk);
    }
}