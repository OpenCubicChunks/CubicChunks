package io.github.opencubicchunks.cubicchunks.utils;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.world.level.dimension.DimensionType;

public class CuboidUtils {

    public static int getCuboidBottomStartY(CubePos pos, DimensionType type) {
        int requestedPosY = pos.getY();
        int cubeCount = Coords.blockToCube(type.height());
        int remainder = requestedPosY % cubeCount;
        return (requestedPosY - remainder);
    }

    public static int getCuboidHeight(DimensionType type) {
        return Coords.blockToCube(type.height());
    }
}
