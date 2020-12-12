package io.github.opencubicchunks.cubicchunks.chunk.storage;

import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;

public interface ISectionStorage {

    void flush(CubePos cubePos);

}
