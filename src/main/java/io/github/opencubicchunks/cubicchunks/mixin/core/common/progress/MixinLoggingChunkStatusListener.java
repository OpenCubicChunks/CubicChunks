package io.github.opencubicchunks.cubicchunks.mixin.core.common.progress;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LoggingChunkStatusListener.class)
public abstract class MixinLoggingChunkStatusListener implements ICubeStatusListener {

    private int loadedCubes;
    private int cubesToGenerate;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(int serverChunkRadius, CallbackInfo ci) {
        // Server chunk radius, divided by CUBE_DIAMETER to get the radius in cubes
        // Multiply by two to convert cube radius -> diameter,
        // And then add one for the center cube
        int cubesEdgeLength = (1+(int) Math.ceil((serverChunkRadius-1) / ((float) IBigCube.CUBE_DIAMETER)))*2+1;
        cubesToGenerate = cubesEdgeLength*cubesEdgeLength*cubesEdgeLength;
    }

    @Override public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        if (newStatus == ChunkStatus.FULL) {
            this.loadedCubes++;
        }
    }

    @Inject(method = "getPercentDone", at = @At("HEAD"), cancellable = true)
    public void getPercentDone(CallbackInfoReturnable<Integer> cir) {
        int percentage = MathHelper.floor(this.loadedCubes * 100.0F / cubesToGenerate);
        cir.setReturnValue(percentage);
    }
}
