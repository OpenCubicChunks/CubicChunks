package io.github.opencubicchunks.cubicchunks.core.asm.mixin.selectable.client.optifine;

import io.github.opencubicchunks.cubicchunks.api.world.ICube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.asm.optifine.IOptifineRenderChunk;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderChunk.class)
public abstract class MixinRenderChunk implements IOptifineRenderChunk {

    @Shadow @Final private BlockPos.MutableBlockPos position;
    @Shadow private World world;
    @Shadow private RenderChunk[] renderChunkNeighboursValid;
    @Shadow private RenderChunk[] renderChunkNeighbours;

    @Shadow public abstract BlockPos getPosition();

    private ICube cube;
    private boolean isCubic;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(World worldIn, RenderGlobal renderGlobalIn, int indexIn, CallbackInfo cbi) {
        this.isCubic = ((ICubicWorld) worldIn).isCubicWorld();
    }

    @Inject(method = "setPosition", at = @At(value = "FIELD", target = "chunk"))
    private void onSetChunk(int x, int y, int z, CallbackInfo cbi) {
        this.cube = null;
        this.isCubic = ((ICubicWorld) world).isCubicWorld();
    }

    @Inject(method = "updateRenderChunkNeighboursValid", at = @At("HEAD"))
    private void onUpdateNeighbors(CallbackInfo cbi) {
        if (!isCubic) {
            return;
        }
        int y = this.getPosition().getY();
        int up = EnumFacing.UP.ordinal();
        int down = EnumFacing.DOWN.ordinal();
        this.renderChunkNeighboursValid[up] = this.renderChunkNeighbours[up].getPosition().getY() == y + 16 ?
                this.renderChunkNeighbours[up] : null;
        this.renderChunkNeighboursValid[down] = this.renderChunkNeighbours[down].getPosition().getY() == y - 16 ?
                this.renderChunkNeighbours[down] : null;
    }

    @ModifyArg(method = "preRenderBlocks",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/BufferBuilder;setTranslation(DDD)V", ordinal = 0),
            index = 1
    )
    private double getRegionY(double dy) {
        return 0;
    }

    @Override public ICube getCube() {
        return this.getCube(this.position);
    }

    @Override public boolean isCubic() {
        return isCubic;
    }

    private ICube getCube(BlockPos posIn) {
        ICube cubeLocal = this.cube;
        if (cubeLocal != null && cubeLocal.isCubeLoaded()) {
            return cubeLocal;
        } else {
            cubeLocal = ((ICubicWorld) this.world).getCubeFromBlockCoords(posIn);
            this.cube = cubeLocal;
            return cubeLocal;
        }
    }

}
