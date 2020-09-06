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
    @Shadow
    public ChunkRenderDispatcher.ChunkRender[] renderChunks;
    @Shadow
    protected int countChunksY;
    @Shadow
    protected int countChunksX;
    @Shadow
    protected int countChunksZ;

    @Shadow protected abstract int getIndex(int x, int y, int z);

    /**
     * @author Barteks2x
     * @reason vertical view distance = horizontal
     */
    @Overwrite
    protected void setCountChunksXYZ(int renderDistanceChunks) {
        int d = renderDistanceChunks * 2 + 1;
        this.countChunksX = d;
        this.countChunksY = d;
        this.countChunksZ = d;
    }

    @Inject(method = "updateChunkPositions", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void setCountChunkXYZ(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        Entity view = Minecraft.getInstance().getCameraEntity();
        double x = view.getX();
        double y = view.getY();
        double z = view.getZ();
        int viewX = MathHelper.floor(x);
        int viewZ = MathHelper.floor(z);
        int viewY = MathHelper.floor(y);

        for (int xIndex = 0; xIndex < this.countChunksX; ++xIndex) {
            int xBase = this.countChunksX * 16;
            int xTemp = viewX - 8 - xBase / 2;
            int posX = xTemp + Math.floorMod(xIndex * 16 - xTemp, xBase);

            for (int zIndex = 0; zIndex < this.countChunksZ; ++zIndex) {
                int zBase = this.countChunksZ * 16;
                int zTemp = viewZ - 8 - zBase / 2;
                int posZ = zTemp + Math.floorMod(zIndex * 16 - zTemp, zBase);

                for (int yIndex = 0; yIndex < this.countChunksY; ++yIndex) {
                    int yBase = this.countChunksY * 16;
                    int yTemp = viewY - 8 - yBase / 2;
                    int posY = yTemp + Math.floorMod(yIndex * 16 - yTemp, yBase);
                    ChunkRenderDispatcher.ChunkRender chunkrenderdispatcher$chunkrender = this.renderChunks[this.getIndex(xIndex, yIndex, zIndex)];
                    chunkrenderdispatcher$chunkrender.setOrigin(posX, posY, posZ);
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "getRenderChunk", at = @At(value = "HEAD"), cancellable = true)
    private void getRenderChunk(BlockPos pos, CallbackInfoReturnable<ChunkRenderDispatcher.ChunkRender> cbi) {
        int x = MathHelper.intFloorDiv(pos.getX(), 16);
        int y = MathHelper.intFloorDiv(pos.getY(), 16);
        int z = MathHelper.intFloorDiv(pos.getZ(), 16);
        x = MathHelper.positiveModulo(x, this.countChunksX);
        y = MathHelper.positiveModulo(y, this.countChunksY);
        z = MathHelper.positiveModulo(z, this.countChunksZ);
        ChunkRenderDispatcher.ChunkRender renderChunk = this.renderChunks[this.getIndex(x, y, z)];
        cbi.cancel();
        cbi.setReturnValue(renderChunk);
    }
}