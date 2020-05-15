package cubicchunks.cc.mixin.core.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ViewFrustum.class)
public class MixinViewFrustum {
    @Shadow
    public ChunkRenderDispatcher.ChunkRender[] renderChunks;
    @Shadow
    protected int countChunksY;
    @Shadow
    protected int countChunksX;
    @Shadow
    protected int countChunksZ;

    @Inject(method = "updateChunkPositions", at = @At(value = "HEAD"), cancellable = true, require = 1)
    private void setCountChunkXYZ(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        Entity view = Minecraft.getInstance().getRenderViewEntity();
        double x = view.getPosX();
        double y = view.getPosY();
        double z = view.getPosZ();
        int viewX = MathHelper.floor(x);
        int viewZ = MathHelper.floor(z);
        int viewY = MathHelper.floor(y);

        for (int xIndex = 0; xIndex < this.countChunksX; ++xIndex) {
            int l = this.countChunksX * 16;
            int i1 = viewX - 8 - l / 2;
            int j1 = i1 + Math.floorMod(xIndex * 16 - i1, l);

            for (int zIndex = 0; zIndex < this.countChunksZ; ++zIndex) {
                int l1 = this.countChunksZ * 16;
                int i2 = viewZ - 8 - l1 / 2;
                int j2 = i2 + Math.floorMod(zIndex * 16 - i2, l1);

                for (int yIndex = 0; yIndex < this.countChunksY; ++yIndex) {
                    int l2 = this.countChunksY * 16;
                    int i3 = viewY - 8 - l2 / 2;
                    int j3 = i3 + Math.floorMod(yIndex * 16 - i3, l2);
                    ChunkRenderDispatcher.ChunkRender chunkrenderdispatcher$chunkrender = this.renderChunks[this.getIndex(xIndex, yIndex, zIndex)];
                    chunkrenderdispatcher$chunkrender.setPosition(j1, j3, j2);
                }
            }
        }
    }

    @Inject(method = "getRenderChunk", at = @At(value = "HEAD"), cancellable = true)
    private void getRenderChunk(BlockPos pos, CallbackInfoReturnable<ChunkRenderDispatcher.ChunkRender> cbi) {
        int x = MathHelper.intFloorDiv(pos.getX(), 16);
        int y = MathHelper.intFloorDiv(pos.getY(), 16);
        int z = MathHelper.intFloorDiv(pos.getZ(), 16);
        x %= this.countChunksX;
        if (x < 0) {
            x += this.countChunksX;
        }
        y %= this.countChunksY;
        if (y < 0) {
            y += this.countChunksY;
        }
        z %= this.countChunksZ;
        if (z < 0) {
            z += this.countChunksZ;
        }
        final int index = (z * this.countChunksY + y) * this.countChunksX + x;
        ChunkRenderDispatcher.ChunkRender renderChunk = this.renderChunks[index];
        cbi.cancel();
        cbi.setReturnValue(renderChunk);
    }

    private int getIndex(int x, int y, int z) {
        return (z * this.countChunksY + y) * this.countChunksX + x;
    }
}
