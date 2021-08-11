package io.github.opencubicchunks.cubicchunks.utils;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;

public class CuboidUtil {

    public static int getMinCubeYForCuboid(int y, int sectionCount) {
        int cuboidSize = (sectionCount / IBigCube.DIAMETER_IN_SECTIONS);
        int localYForCuboid = y & cuboidSize;
        return (y - localYForCuboid);
    }

    public static int getMaxCubeYForCuboid(int y, int sectionCount) {
        int cuboidSize = (sectionCount / IBigCube.DIAMETER_IN_SECTIONS);
        int localYForCuboid = y & cuboidSize;
        return (y - localYForCuboid) + cuboidSize;
    }

    public static int getCuboidCubeHeight(int sectionCount) {
        return sectionCount / IBigCube.DIAMETER_IN_SECTIONS;
    }
}
