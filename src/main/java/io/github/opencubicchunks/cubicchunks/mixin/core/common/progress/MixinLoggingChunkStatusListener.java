package io.github.opencubicchunks.cubicchunks.mixin.core.common.progress;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LoggingChunkStatusListener.class)
public abstract class MixinLoggingChunkStatusListener implements ICubeStatusListener {

    private int loadedCubes;
    private int cubesToGenerate;
    private int chunksToGenerate;
    
    @Shadow private int loadedChunks;
    
    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(int vanillaSpawnRadius, CallbackInfo ci) {
        // Server chunk radius, divided by CUBE_DIAMETER to get the radius in cubes
        // Except we subtract one before the ceil and readd it after, for... some reason
        // Multiply by two to convert cube radius -> diameter,
        // And then add one for the center cube
        int ccCubeRadius = 1+(int) Math.ceil((vanillaSpawnRadius-1) / ((float) IBigCube.DIAMETER_IN_SECTIONS));
        int ccCubeDiameter = ccCubeRadius*2+1;
        cubesToGenerate = ccCubeDiameter*ccCubeDiameter*ccCubeDiameter;
        
        int ccChunkRadius = ccCubeRadius * IBigCube.DIAMETER_IN_SECTIONS;
        int ccChunkDiameter = ccChunkRadius*2+1;
        chunksToGenerate = ccChunkDiameter*ccChunkDiameter;
    }
    
    @Override public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
    	if (newStatus == ChunkStatus.FULL) {
            this.loadedCubes++;
        }
    }

    @Inject(method = "getPercentDone", at = @At("HEAD"), cancellable = true)
    public void getPercentDone(CallbackInfoReturnable<Integer> cir) {
        int loaded = loadedChunks + loadedCubes;
        int total = chunksToGenerate + cubesToGenerate;
        int percentage = MathHelper.floor(loaded * 100.0F / total);
        cir.setReturnValue(percentage);
    }
}
