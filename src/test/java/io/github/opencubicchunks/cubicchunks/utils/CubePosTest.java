package io.github.opencubicchunks.cubicchunks.utils;

import static org.junit.Assert.assertEquals;

import io.github.opencubicchunks.cubicchunks.world.level.CubePos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.junit.Test;

public class CubePosTest {

    @Test
    public void cubePosTest() {
        chunkPosTest();
        sectionPosTest();
    }

    public void chunkPosTest() {
        for (int x = Integer.MIN_VALUE / 16; x < Integer.MAX_VALUE / 16; x += Integer.MAX_VALUE / 16 / 500) {
            for (int y = Integer.MIN_VALUE / 16; y < Integer.MAX_VALUE / 16; y += Integer.MAX_VALUE / 16 / 500) {
                for (int z = Integer.MIN_VALUE / 16; z < Integer.MAX_VALUE / 16; z += Integer.MAX_VALUE / 16 / 500) {
                    ChunkPos chunkPos = CubePos.of(x, y, z).asChunkPos();
                    CubePos cubePos = CubePos.from(chunkPos, y);

                    assertEquals(x, cubePos.getX());
                    assertEquals(z, cubePos.getZ());
                }
            }
        }
    }

    public void sectionPosTest() {
        for (int x = Integer.MIN_VALUE / 16; x < Integer.MAX_VALUE / 16; x += Integer.MAX_VALUE / 16 / 500) {
            for (int y = Integer.MIN_VALUE / 16; y < Integer.MAX_VALUE / 16; y += Integer.MAX_VALUE / 16 / 500) {
                for (int z = Integer.MIN_VALUE / 16; z < Integer.MAX_VALUE / 16; z += Integer.MAX_VALUE / 16 / 500) {
                    SectionPos sectionPos = CubePos.of(x, y, z).asSectionPos();
                    CubePos cubePos = CubePos.from(sectionPos);

                    assertEquals(x, cubePos.getX());
                    assertEquals(y, cubePos.getY());
                    assertEquals(z, cubePos.getZ());
                }
            }
        }
    }
}
