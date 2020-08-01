package io.github.opencubicchunks.cubicchunks.mixin.core.common.progress;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.ICubeStatusListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(LoggingChunkStatusListener.class)
public abstract class MixinLoggingChunkStatusListener implements ICubeStatusListener {

    private int loadedCubes;
    private int totalCubes;

    @Shadow private int loadedChunks;
    @Shadow @Final
    @Mutable
    private int totalChunks;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(int vanillaSpawnRadius, CallbackInfo ci) {
        // Server chunk radius, divided by CUBE_DIAMETER to get the radius in cubes
        // Except we subtract one before the ceil and readd it after, for... some reason
        // Multiply by two to convert cube radius -> diameter,
        // And then add one for the center cube
        int ccCubeRadius = 1+(int) Math.ceil((vanillaSpawnRadius-1) / ((float) IBigCube.DIAMETER_IN_SECTIONS));
        int ccCubeDiameter = ccCubeRadius*2+1;
        totalCubes = ccCubeDiameter*ccCubeDiameter*ccCubeDiameter;

        int ccChunkRadius = ccCubeRadius * IBigCube.DIAMETER_IN_SECTIONS;
        int ccChunkDiameter = ccChunkRadius*2+1;
        totalChunks = ccChunkDiameter*ccChunkDiameter;
    }

    @Override public void startCubes(CubePos center) {}

    @Override public void cubeStatusChanged(CubePos cubePos, @Nullable ChunkStatus newStatus) {
        if (newStatus == ChunkStatus.FULL) {
            this.loadedCubes++;
        }
    }

    /**
     * @author CursedFlames & NotStirred
     * @reason number of chunks is different due to rounding to chunks rounding to 1 cubes to 1, 2, 4, 8 depending on {@link IBigCube#DIAMETER_IN_SECTIONS}
     */
    @Overwrite
    public int getPercentDone() {
        int loaded = loadedChunks + loadedCubes;
        int total = totalChunks + totalCubes;
        return MathHelper.floor(loaded * 100.0F / total);
    }
}
