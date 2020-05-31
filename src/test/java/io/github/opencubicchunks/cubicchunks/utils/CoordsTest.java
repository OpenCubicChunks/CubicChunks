package io.github.opencubicchunks.cubicchunks.utils;

import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class CoordsTest {
    @Test
    public void testBlockToIndex32() {
        int idx = Coords.blockToIndex32(0, 0, 0);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    assertEquals(idx, Coords.blockToIndex32(x, y, z));
                }
            }
        }

        Set<Integer> coords = new HashSet<>();
        for (int i = 0; i < 8; i++) {
            coords.add(i);
        }
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                for (int z = 0; z < 2; z++) {
                    int v = Coords.blockToIndex32(x*16, y*16, z*16);
                    assertThat(v, is(in(coords)));
                    coords.remove(v);
                }
            }
        }
    }
}