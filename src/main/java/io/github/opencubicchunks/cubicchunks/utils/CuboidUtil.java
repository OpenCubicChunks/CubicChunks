package io.github.opencubicchunks.cubicchunks.utils;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;

public class CuboidUtil {

    public static int getMinCubeYForCuboid(int y, Level level) {
        return getMinCubeYForCuboid(y, SectionPos.blockToSectionCoord(level.dimensionType().height()));
    }

    public static int getMaxCubeYForCuboid(int y, Level level) {
        return getMaxCubeYForCuboid(y, SectionPos.blockToSectionCoord(level.dimensionType().height()));
    }

    private static int getMinCubeYForCuboid(int y, int sectionCount) {
        int cuboidSize = (sectionCount / IBigCube.DIAMETER_IN_SECTIONS);
        int localYForCuboid = y & cuboidSize;
        return (y - localYForCuboid);
    }

    private static int getMaxCubeYForCuboid(int y, int sectionCount) {
        int cuboidSize = (sectionCount / IBigCube.DIAMETER_IN_SECTIONS);
        int localYForCuboid = y & cuboidSize;
        return (y - localYForCuboid) + cuboidSize;
    }

    public static int getCuboidCubeHeight(int sectionCount) {
        return sectionCount / IBigCube.DIAMETER_IN_SECTIONS;
    }

    public static CubePos getMinCubeFromRequested(CubePos requested, int sectionCount) {
        return CubePos.of(requested.getX(), getMinCubeYForCuboid(requested.getY(), sectionCount), requested.getZ());
    }

    public static CubePos getMinCubeFromRequested(CubePos requested, Level level) {
        return getMinCubeFromRequested(requested, SectionPos.blockToSectionCoord(level.dimensionType().height()));
    }
}
