package io.github.opencubicchunks.cubicchunks.server;

import io.github.opencubicchunks.cubicchunks.chunk.cube.BigCube;

public interface ICubicWorld { //TODO: maybe rename this class? I think this name is reserved by the API
    BigCube getCube(int cubeX, int cubeY, int cubeZ);
}
