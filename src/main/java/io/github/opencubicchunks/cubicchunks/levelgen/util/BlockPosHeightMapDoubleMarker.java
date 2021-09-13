package io.github.opencubicchunks.cubicchunks.levelgen.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public class BlockPosHeightMapDoubleMarker extends BlockPos {
    private final boolean isEmpty;

    public BlockPosHeightMapDoubleMarker(int i, int j, int k, boolean isEmpty) {
        super(i, j, k);
        this.isEmpty = isEmpty;
    }

    public BlockPosHeightMapDoubleMarker(double d, double e, double f, boolean isEmpty) {
        super(d, e, f);
        this.isEmpty = isEmpty;
    }

    public BlockPosHeightMapDoubleMarker(Vec3 vec3, boolean isEmpty) {
        super(vec3);
        this.isEmpty = isEmpty;
    }

    public BlockPosHeightMapDoubleMarker(Position position, boolean isEmpty) {
        super(position);
        this.isEmpty = isEmpty;
    }

    public BlockPosHeightMapDoubleMarker(Vec3i vec3i, boolean isEmpty) {
        super(vec3i);
        this.isEmpty = isEmpty;
    }

    public boolean isEmpty() {
        return isEmpty;
    }
}
