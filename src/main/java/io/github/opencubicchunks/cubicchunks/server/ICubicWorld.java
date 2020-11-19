package io.github.opencubicchunks.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkStatus;

import javax.annotation.Nullable;

public interface ICubicWorld { //TODO: maybe rename this class? I think this name is reserved by the API
    IBigCube getCube(int cubeX, int cubeY, int cubeZ);

    default IBigCube getCube(CubePos cubePos) {
        return getCube(cubePos.getX(), cubePos.getY(), cubePos.getZ());
    }

    IBigCube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus status);

    default IBigCube getCube(CubePos cubePos, ChunkStatus status) {
        return getCube(cubePos.getX(), cubePos.getY(), cubePos.getZ(), status);
    }

    default IBigCube getCube(BlockPos pos) {
        return getCube(CubePos.from(pos));
    }

    @Nullable
    IBigCube getCube(int cubeX, int cubeY, int cubeZ, ChunkStatus status, boolean notnull);
}