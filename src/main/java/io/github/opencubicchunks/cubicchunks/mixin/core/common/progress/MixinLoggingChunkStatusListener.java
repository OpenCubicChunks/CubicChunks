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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LoggingChunkStatusListener.class)
public abstract class MixinLoggingChunkStatusListener implements ICubeStatusListener {
    // Server view radius, divided by CUBE_DIAMETER to get the radius in cubes
    // Multiply by two to convert cube radius -> diameter,
    // And then add three for the center cube, and boundary cubes that are generated but not loaded.
    private static final int CUBES_EDGE_LENGTH = ((int) Math.ceil(10 / ((float) IBigCube.CUBE_DIAMETER)))*2+3;
    private static final int CUBES_TO_GENERATE = CUBES_EDGE_LENGTH*CUBES_EDGE_LENGTH*CUBES_EDGE_LENGTH;

    private int loadedCubes;

    @Override public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        if (newStatus == ChunkStatus.FULL) {
            this.loadedCubes++;
        }
    }

    @Inject(method = "getPercentDone", at = @At("HEAD"), cancellable = true)
    public void getPercentDone(CallbackInfoReturnable<Integer> cir) {
        int percentage = MathHelper.floor(this.loadedCubes * 100.0F / CUBES_TO_GENERATE);
        cir.setReturnValue(percentage);
    }
}
