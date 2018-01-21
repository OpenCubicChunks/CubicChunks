/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.Lists;
import cubicchunks.util.Coords;
import cubicchunks.world.ServerSurfaceTracker;
import cubicchunks.world.SurfaceTracker;
import mcp.MethodsReturnNonnullByDefault;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class TestSurfaceTracker {

    private static Field YminField;
    private static Field YmaxField;
    private static Field SegmentsField;

    static {
        try {
            YminField = ServerSurfaceTracker.class.getDeclaredField("ymin");
            YminField.setAccessible(true);
            YmaxField = ServerSurfaceTracker.class.getDeclaredField("ymax");
            YmaxField.setAccessible(true);
            SegmentsField = ServerSurfaceTracker.class.getDeclaredField("segments");
            SegmentsField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ex) {
            throw new Error(ex);
        }
    }

    @Test
    public void getWithAllTransparent() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
        assertEquals(Coords.NO_HEIGHT, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void getWithoutDataSingleBlock() {
        ServerSurfaceTracker surfaceTracker = makeIndex(10, 10);
        assertEquals(10, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void getWithoutDataMultipleBlocks() {
        ServerSurfaceTracker surfaceTracker = makeIndex(8, 10);
        assertEquals(10, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void getWith1Data() {
        ServerSurfaceTracker surfaceTracker = makeIndex(8, 10,
                8, 1
        );
        assertEquals(10, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void setSingleOpaqueFromEmpty() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
        surfaceTracker.onOpacityChange(0, 10, 0, 255);
        assertEquals(10, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(null, getSegments(surfaceTracker));
    }

    @Test
    public void setSingleTransparentFromSingleOpaque() {
        ServerSurfaceTracker surfaceTracker = makeIndex(10, 10);
        surfaceTracker.onOpacityChange(0, 10, 0, 0);
        assertEquals(Coords.NO_HEIGHT, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(null, getSegments(surfaceTracker));
    }

    @Test
    public void setExpandSingleOpaque() {
        ServerSurfaceTracker surfaceTracker = makeIndex(10, 10);

        surfaceTracker.onOpacityChange(0, 11, 0, 255);
        assertEquals(11, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(null, getSegments(surfaceTracker));

        surfaceTracker.onOpacityChange(0, 9, 0, 255);
        assertEquals(11, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(null, getSegments(surfaceTracker));
    }

    @Test
    public void setShrinkOpaque() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 11);

        surfaceTracker.onOpacityChange(0, 9, 0, 0);
        assertEquals(11, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(null, getSegments(surfaceTracker));

        surfaceTracker.onOpacityChange(0, 11, 0, 0);
        assertEquals(10, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(null, getSegments(surfaceTracker));
    }

    @Test
    public void setDisjointOpaqueAboveOpaue() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 11);

        surfaceTracker.onOpacityChange(0, 16, 0, 255);
        assertEquals(16, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                9, 1,
                12, 0,
                16, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDisjointOpaqueBelowOpaue() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 11);

        surfaceTracker.onOpacityChange(0, 4, 0, 255);
        assertEquals(11, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                4, 1,
                5, 0,
                9, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDisjointOpaqueAboveOpaques() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 16,
                9, 1,
                12, 0,
                16, 1
        );

        surfaceTracker.onOpacityChange(0, 20, 0, 255);
        assertEquals(20, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                9, 1,
                12, 0,
                16, 1,
                17, 0,
                20, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDisjointOpaqueBelowOpaques() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 16,
                9, 1,
                12, 0,
                16, 1
        );

        surfaceTracker.onOpacityChange(0, 3, 0, 255);
        assertEquals(16, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                3, 1,
                4, 0,
                9, 1,
                12, 0,
                16, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void extendTopOpaqueUp() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 16,
                9, 1,
                12, 0,
                16, 1
        );

        surfaceTracker.onOpacityChange(0, 17, 0, 255);
        assertEquals(17, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                9, 1,
                12, 0,
                16, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void extendBottomOpaqueDown() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 16,
                9, 1,
                12, 0,
                16, 1
        );

        surfaceTracker.onOpacityChange(0, 8, 0, 255);
        assertEquals(16, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                8, 1,
                12, 0,
                16, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setBisectOpaue() {
        ServerSurfaceTracker surfaceTracker = makeIndex(9, 11);

        surfaceTracker.onOpacityChange(0, 10, 0, 0);
        assertEquals(11, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                9, 1,
                10, 0,
                11, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDataStartSameAsBottomRoomBeforeTop() {
        ServerSurfaceTracker surfaceTracker = makeIndex(4, 7,
                4, 1
        );

        surfaceTracker.onOpacityChange(0, 4, 0, 0);
        assertEquals(7, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                5, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDataNotStartSameAsNextRoomAfter() {
        ServerSurfaceTracker surfaceTracker = makeIndex(2, 7,
                2, 1
        );

        surfaceTracker.onOpacityChange(0, 4, 0, 0);
        assertEquals(7, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                2, 1,
                4, 0,
                5, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDataNotStartSameAsTopNoRoomAfter() {
        ServerSurfaceTracker surfaceTracker = makeIndex(2, 4,
                2, 1
        );

        surfaceTracker.onOpacityChange(0, 4, 0, 0);
        assertEquals(3, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                2, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setDataAfterTopSameAsPrevious() {
        ServerSurfaceTracker surfaceTracker = makeIndex(2, 4,
                2, 1
        );

        surfaceTracker.onOpacityChange(0, 4, 0, 0);
        assertEquals(3, surfaceTracker.getTopBlockY(0, 0));
        assertEquals(Arrays.asList(
                2, 1
        ), getSegments(surfaceTracker));
    }

    @Test
    public void setTransparentInOpaqueAndClear() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();

        // place blocks
        surfaceTracker.onOpacityChange(0, 0, 0, 255);
        surfaceTracker.onOpacityChange(0, 1, 0, 255);
        surfaceTracker.onOpacityChange(0, 2, 0, 255);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        // remove the middle one
        surfaceTracker.onOpacityChange(0, 1, 0, 0);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        // remove the bottom one
        surfaceTracker.onOpacityChange(0, 0, 0, 0);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        // remove the top one
        surfaceTracker.onOpacityChange(0, 2, 0, 0);

        assertEquals(Coords.NO_HEIGHT, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void checkFloatingIsland() {

        // make a surfaceTracker with a surface
        ServerSurfaceTracker surfaceTracker = makeIndex(100, 102);

        // start setting blocks in the sky
        surfaceTracker.onOpacityChange(0, 200, 0, 255);

        assertEquals(200, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 201, 0, 255);

        assertEquals(201, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 202, 0, 255);

        assertEquals(202, surfaceTracker.getTopBlockY(0, 0));
    }

    //test 21011120
    @Test
    public void testMergeSegmentsIntoNoSegmentsAndRemoveTop_generated() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
        surfaceTracker.onOpacityChange(0, 2, 0, 1);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 1);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 1, 0, 1);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 2, 0, 0);

        assertEquals(1, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void test41210140110000() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
        surfaceTracker.onOpacityChange(0, 4, 0, 1);

        assertEquals(4, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 2, 0, 1);

        assertEquals(4, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 1);

        assertEquals(4, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 4, 0, 0);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 1, 0, 1);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 0);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 0);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void testSetAndClear_generated() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
        surfaceTracker.onOpacityChange(0, 4, 0, 1);

        assertEquals(4, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 2, 0, 1);

        assertEquals(4, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 1);

        assertEquals(4, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 4, 0, 0);

        assertEquals(2, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 2, 0, 0);

        assertEquals(0, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 0);

        assertEquals(Coords.NO_HEIGHT, surfaceTracker.getTopBlockY(0, 0));

        surfaceTracker.onOpacityChange(0, 0, 0, 0);

        assertEquals(Coords.NO_HEIGHT, surfaceTracker.getTopBlockY(0, 0));
    }

    @Test
    public void test31110000hmaplim_generated() {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
        surfaceTracker.onOpacityChange(0, 3, 0, 1);
        surfaceTracker.onOpacityChange(0, 1, 0, 1);

        int height = surfaceTracker.getTopBlockYBelow(0, 0, 3);
        assertEquals(1, height);
    }

    @Test
    public void allCombinationsTest() {
        //tested with value up to 6 (takes a lot of time)
        final int maxHeight = 5, numBlocks = 4;
        int[] yPosOpacityEncoded = new int[numBlocks];

        while (true) {
            ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();
            ArraySurfaceTracker test = new ArraySurfaceTracker();

            StringBuilder msg = new StringBuilder();
            for (int encoded : yPosOpacityEncoded) {
                msg.append("i[").append(encoded / 2).append("]=").append(encoded % 2).append(", ");
            }
            String message = msg.toString();
            try {
                for (int i = 0; i < yPosOpacityEncoded.length; i++) {
                    int opacity = yPosOpacityEncoded[i] % 2;
                    surfaceTracker.onOpacityChange(0, yPosOpacityEncoded[i] / 2, 0, opacity);
                    test.set(yPosOpacityEncoded[i] / 2, opacity);
                }
                //store-read-store test
                byte[] b = surfaceTracker.getData();
                ServerSurfaceTracker newSurfaceTracker = new ServerSurfaceTracker();
                newSurfaceTracker.readData(b);
                assertArrayEquals("Got different data after creating surfaceTracker based on read data\n" + message + "\n", b, newSurfaceTracker.getData());
            } catch (Throwable t) {
                System.out.println(message + "exception");
                throw t;
            }
            for (int i = 0; i < maxHeight; i++) {
                assertEquals(message + ", maxHBelow(" + i + ")", (Integer) test.getMaxYBelow(i), (Integer) surfaceTracker.getTopBlockYBelow(0, 0, i));
            }

            assertEquals(message + "maxY", (Integer) test.getMaxY(), (Integer) surfaceTracker.getTopBlockY(0, 0));


            yPosOpacityEncoded[0]++;
            for (int i = 0; i < numBlocks - 1; i++) {
                if (yPosOpacityEncoded[i] == maxHeight * 2) {
                    yPosOpacityEncoded[i] = 0;
                    yPosOpacityEncoded[i + 1]++;
                }
            }
            if (yPosOpacityEncoded[numBlocks - 1] == maxHeight * 2) {
                break;
            }
        }
    }

    private ServerSurfaceTracker makeIndex(int ymin, int ymax, int... segments) {
        ServerSurfaceTracker surfaceTracker = new ServerSurfaceTracker();

        // pack the segments
        int[] packedSegments = null;
        if (segments.length > 0) {
            packedSegments = new int[segments.length / 2];
            for (int i = 0; i < segments.length / 2; i++) {
                packedSegments[i] = segments[i * 2 + 0];
            }
        }

        set(surfaceTracker, ymin, ymax, packedSegments);
        return surfaceTracker;
    }

    private void set(ServerSurfaceTracker surfaceTracker, int ymin, int ymax, int[] segments) {
        try {
            ((int[]) YminField.get(surfaceTracker))[0] = ymin;
            ((SurfaceTracker.HeightMap) YmaxField.get(surfaceTracker)).set(0, ymax);
            ((int[][]) SegmentsField.get(surfaceTracker))[0] = segments;
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new Error(ex);
        }
    }

    private static int getOpacity(int segmentIndex) {
        return (segmentIndex + 1) % 2;
    }

    private List<Integer> getSegments(ServerSurfaceTracker surfaceTracker) {
        try {
            int[] packedSegments = ((int[][]) SegmentsField.get(surfaceTracker))[0];
            if (packedSegments == null) {
                return null;
            }

            final int NoneSegment = 0x7fffff;

            // unpack the segments
            List<Integer> segments = Lists.newArrayList();
            for (int i = 0; i < packedSegments.length && packedSegments[i] != NoneSegment; i++) {
                segments.add(packedSegments[i]);
                segments.add(getOpacity(i));
            }
            return segments;

        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new Error(ex);
        }
    }

    private static class ArraySurfaceTracker {

        private int[] arr = new int[100];

        private void set(int y, int val) {
            arr[y] = val;
        }

        public int getMaxY() {
            for (int i = arr.length - 1; i >= 0; i--) {
                if (arr[i] != 0) {
                    return i;
                }
            }
            return Coords.NO_HEIGHT;
        }

        public int getMaxYBelow(int y) {
            for (int i = y - 1; i >= 0; i--) {
                if (arr[i] != 0) {
                    return i;
                }
            }
            return Coords.NO_HEIGHT;
        }
    }
}
