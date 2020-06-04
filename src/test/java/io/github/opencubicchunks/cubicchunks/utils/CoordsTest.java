package io.github.opencubicchunks.cubicchunks.utils;

import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class CoordsTest {
    @Test
    public void testBlockToIndex32() {
        int idx = Coords.blockToIndex(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    assertEquals(idx, Coords.blockToIndex(x, y, z));
                }
            }
        }

        Set<Integer> coords = new HashSet<>();
        for (int i = 0; i < (ICube.CUBE_DIAMETER * ICube.CUBE_DIAMETER * ICube.CUBE_DIAMETER); i++) {
            coords.add(i);
        }
        for (int x = 0; x < ICube.CUBE_DIAMETER; x++) {
            for (int y = 0; y < ICube.CUBE_DIAMETER; y++) {
                for (int z = 0; z < ICube.CUBE_DIAMETER; z++) {
                    int v = Coords.blockToIndex(x*16, y*16, z*16);

                    System.out.printf("%d %d %d\n", x, y, z);
                    assertThat(v, is(in(coords)));
                    coords.remove(v);
                }
            }
        }
    }

    @Test
    public void testBlockToIndex()
    {
        for (int x = 0; x < ICube.CUBE_DIAMETER; x++) {
            for (int y = 0; y < ICube.CUBE_DIAMETER; y++) {
                for (int z = 0; z < ICube.CUBE_DIAMETER; z++) {
                    int index = Coords.blockToIndex(x*16, y*16, z*16);
                    assertEquals(x, Coords.indexToX(index));
                    assertEquals(y, Coords.indexToY(index));
                    assertEquals(z, Coords.indexToZ(index));
                }
            }
        }

    }
}