package io.github.opencubicchunks.cubicchunks.world.lighting;

public interface ISkyLightColumnChecker {
    void checkSkyLightColumn(int x, int z, int oldHeight, int newHeight);
}
