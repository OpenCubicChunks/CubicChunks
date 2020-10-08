/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.world;

import io.github.opencubicchunks.cubicchunks.api.util.Coords;
import io.github.opencubicchunks.cubicchunks.api.world.IHeightMap;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ServerHeightMap implements IHeightMap {

    /**
     * Special value to indicate the absence of a segment in the segments arrays.
     * Since all integer values might represent a valid segment, an unlikely value to occur has been chosen. It
     * logically represents a 1-block segment at the very top of the world.
     */
    private static final int NONE_SEGMENT = 0x7fffffff;

    /**
     * Array containing the y-coordinates of the lowest segment in each block column. The value {@link Coords#NO_HEIGHT}
     * is used if a given block column does not contain any segments.
     */
    @Nonnull private final int[] ymin;

    /**
     * Array containing the y-coordinate of the highest segment in each block column. The value {@link Coords#NO_HEIGHT}
     * is used if a given block column does not contain any segments.
     */
    @Nonnull private final HeightMap ymax;

    /**
     * Array containing an array of segments for each x/z position in a column.
     */
    @Nonnull private final int[][] segments;

    private int heightMapLowest;

    public ServerHeightMap(int[] heightmap) {
        this.ymin = new int[Cube.SIZE * Cube.SIZE];
        this.ymax = new HeightMap(heightmap);

        this.segments = new int[Cube.SIZE * Cube.SIZE][];

        // init to empty
        for (int i = 0; i < Cube.SIZE * Cube.SIZE; i++) {
            this.ymin[i] = Coords.NO_HEIGHT;
            this.ymax.set(i, Coords.NO_HEIGHT);
        }

        this.heightMapLowest = Coords.NO_HEIGHT;
    }


    private static int getOpacity(int segmentIndex) {
        return (segmentIndex + 1) % 2;
    }

    private static int getLastSegmentIndex(int[] segments) {
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != NONE_SEGMENT) {
                return i;
            }
        }
        throw new Error("Invalid segments state");
    }

    /**
     * Checks if the number of segments in a given block-column is correct.
     * The number of segments must always be odd.
     *
     * @param xzIndex The block-column's index.
     * @return True if the number of segments is correct.
     */
    private boolean parityCheck(int xzIndex) {
        return getLastSegmentIndex(segments[xzIndex]) % 2 == 0;
    }

    // Interface: IHeightMap ----------------------------------------------------------------------------------------

    @Override
    public void onOpacityChange(int localX, int blockY, int localZ, int opacity) {
        if (blockY > CubicChunks.MAX_SUPPORTED_BLOCK_Y || blockY < CubicChunks.MIN_SUPPORTED_BLOCK_Y) {
            return;
        }
        int xzIndex = getIndex(localX, localZ);

        // try to stay in no-segments mode as long as we can, this is the simple case
        boolean isOpaque = opacity != 0;
        if (this.segments[xzIndex] == null) {
            this.setNoSegments(xzIndex, blockY, isOpaque);
        } else {
            this.setOpacityWithSegments(xzIndex, blockY, isOpaque);
        }

        this.heightMapLowest = Coords.NO_HEIGHT;
    }

    @Override
    public int getTopBlockY(int localX, int localZ) {
        return this.ymax.get(getIndex(localX, localZ));
    }

    @Override
    public int getTopBlockYBelow(int localX, int localZ, int blockY) {

        // within the highest segment or there exists no segment for this block column
        int i = getIndex(localX, localZ);
        if (blockY > this.ymax.get(i)) {
            return this.getTopBlockY(localX, localZ);
        }

        // below or at the minimum height, thus there are no blocks below
        if (blockY <= this.ymin[i]) {
            return Coords.NO_HEIGHT;
        }

        // There are no opacity changes, everything is opaque from ymin to ymax. blockY is between ymin and ymax, thus
        // the next opaque block below blockY is blockY - 1.
        int[] segments = this.segments[i];
        if (segments == null) {
            return blockY - 1;
        }

        // binary search for the segment containing blockY
        int mini = 0;
        int maxi = getLastSegmentIndex(segments);
        while (mini <= maxi) {
            int midi = (mini + maxi) >>> 1;
            int midPos = segments[midi];

            if (midPos < blockY) {
                mini = midi + 1;
            } else if (midPos > blockY) {
                maxi = midi - 1;
            } else {
                // hit a segment start exactly
                mini = midi + 1;
                break;
            }
        }

        assert (mini > 0) : String.format("can't find %d in %s", blockY, dump(localX, localZ));

        // The binary search ends on answer + 1, so subtract 1. The result is the index of the segment containing
        // blockY.
        int segmentIndex = mini - 1;
        if (segmentIndex < 0) {
            return Coords.NO_HEIGHT;
        }
        int blockYSegment = segments[segmentIndex];
        int blockYSegmentOpacity = getOpacity(segmentIndex);

        // The lowest segment is always opaque. Thus, if blockY is in the lowest segment, the next opaque block is
        // at blockY - 1.
        if (segmentIndex == 0) {
            assert blockYSegmentOpacity != 0 : "The bottom opacity segment is transparent!";
            return blockY - 1;
        }

        // Otherwise, there exists a segment underneath the segment of blockY.

        // If the segment of blockY is transparent, the next opaque block is at the top of the segment underneath.
        if (blockYSegmentOpacity == 0) {
            return blockYSegment - 1;
        }

        // The segment of blockY is opaque, thus, if blockY is not at the bottom of its segment, the next opaque block
        // is at blockY - 1.
        if (blockY != blockYSegment) {
            return blockY - 1;
        }

        // If blockY is the lowest block in its segment, the next opaque block is the highest block in the next opaque
        // segment.
        int belowYSegment = segments[segmentIndex - 1];
        return belowYSegment - 1;
    }
    @Override
    public int getLowestTopBlockY() {
        if (this.heightMapLowest == Coords.NO_HEIGHT) {
            this.heightMapLowest = Integer.MAX_VALUE;
            for (int i = 0; i < Cube.SIZE * Cube.SIZE; i++) {
                if (this.ymax.get(i) < this.heightMapLowest) {
                    this.heightMapLowest = this.ymax.get(i);
                }
            }
            if (this.heightMapLowest == Coords.NO_HEIGHT) {
                this.heightMapLowest--; // don't recalculate this on every call
            }
        }
        return this.heightMapLowest;
    }


    // Helper ----------------------------------------------------------------------------------------------------------

    private void setNoSegments(int xzIndex, int blockY, boolean isOpaque) {
        if (isOpaque) {
            this.setNoSegmentsOpaque(xzIndex, blockY);
        } else {
            this.setNoSegmentsTransparent(xzIndex, blockY);
        }
    }

    private void setNoSegmentsOpaque(int xzIndex, int blockY) {

        // something from nothing?
        if (this.ymin[xzIndex] == Coords.NO_HEIGHT && this.ymax.get(xzIndex) == Coords.NO_HEIGHT) {
            this.ymin[xzIndex] = blockY;
            this.ymax.set(xzIndex, blockY);
            return;
        }

        // extending the range?
        if (blockY == this.ymin[xzIndex] - 1) {
            this.ymin[xzIndex]--;
            return;
        } else if (blockY == this.ymax.get(xzIndex) + 1) {
            this.ymax.increment(xzIndex);
            return;
        }

        // making a new section

        //more than one block above ymax?
        if (blockY > this.ymax.get(xzIndex) + 1) {
            /*
             A visualization of what happens:

             X - already opaque, within min-max
             # - newly set opaque

             [ ]
             --- --> no segment here, new_ymax=blockY
             [#] (block at blockY)
             --- --> segment=2, height=blockY, isOpaque=1
             [ ]
             ---
             [ ]
             --- --> segment=1, height=old_ymax + 1, isOpaque=0
             [X] (block at old_ymax)
             ---
             [X]
             ---
             [X] (block at ymin)
             --- --> segment=0, height=ymin, isOpaque=1
             [ ]
              ^ going up from there
             */
            this.segments[xzIndex] = new int[]{
                    this.ymin[xzIndex],
                    this.ymax.get(xzIndex) + 1,
                    blockY
            };
            this.ymax.set(xzIndex, blockY);
            return;
            //more than one block below ymin?
        } else if (blockY < this.ymin[xzIndex] - 1) {
            /*
             X - already opaque, within min-max
             # - newly set opaque

             ---
             [ ]
             --- --> no segment, limited by ymax
             [X] (block at ymax)
             ---
             [X]
             ---
             [X] (block at old_ymin)
             --- --> segment=2, height=old_ymin, isOpaque=1
             [ ]
             ---
             [ ]
             --- --> segment=1, height=blockY + 1, isOpaque=0
             [#] (block at blockY)
             --- --> segment=0, height=blockY, isOpaque=1
             [ ]
              ^ going up from there
             */
            this.segments[xzIndex] = new int[]{
                    blockY,
                    blockY + 1,
                    this.ymin[xzIndex]
            };
            this.ymin[xzIndex] = blockY;
            return;
        }

        // must already be in range
        assert (blockY >= this.ymin[xzIndex] && blockY <= this.ymax.get(xzIndex));
    }

    private void setNoSegmentsTransparent(int xzIndex, int blockY) {
        // nothing into nothing?
        if (this.ymin[xzIndex] == Coords.NO_HEIGHT && this.ymax.get(xzIndex) == Coords.NO_HEIGHT) {
            return;
        }
        assert !(this.ymin[xzIndex] == Coords.NO_HEIGHT || this.ymax.get(xzIndex) == Coords.NO_HEIGHT) :
                "Only one of ymin and ymax is NONE! This is not possible";

        // only one block left?
        if (this.ymax.get(xzIndex) == this.ymin[xzIndex]) {

            // something into nothing?
            if (blockY == this.ymin[xzIndex]) {
                this.ymin[xzIndex] = Coords.NO_HEIGHT;
                this.ymax.set(xzIndex, Coords.NO_HEIGHT);
            }

            // if setting to transparent somewhere else - nothing changes
            return;
        }

        // out of range?
        if (blockY < this.ymin[xzIndex] || blockY > this.ymax.get(xzIndex)) {
            return;
        }

        // shrinking the range?
        if (blockY == this.ymin[xzIndex]) {
            this.ymin[xzIndex]++;
            return;
        } else if (blockY == this.ymax.get(xzIndex)) {
            this.ymax.decrement(xzIndex);
            return;
        }

        // we must be bisecting the range, need to make segments
        assert (blockY > this.ymin[xzIndex] && blockY <
                this.ymax.get(xzIndex)) :
                String.format("blockY outside of ymin/ymax range: %d -> [%d,%d]", blockY, this.ymin[xzIndex], this.ymax.get(xzIndex));
        /*
         Example:
         ---
         [ ]
         --- --> no segment, limited by ymax
         [X] (block at ymax)
         ---
         [X]
         --- --> segment=2, height=blockY + 1, isOpaque=1
         [-] <--removing this, at y=blockY
         --- --> segment=1, height=blockY, isOpaque=0
         [X]
         ---
         [X] (block ay ymin)
         --- --> segment=0, height=ymin, isOpaque=1
         [ ]
          ^ going up
        */
        this.segments[xzIndex] = new int[]{
                this.ymin[xzIndex],
                blockY,
                blockY + 1
        };
    }

    private void setOpacityWithSegments(int xzIndex, int blockY, boolean isOpaque) {
        // binary search to find the insertion point
        int[] segments = this.segments[xzIndex];
        int minj = 0;
        int maxj = getLastSegmentIndex(segments);
        while (minj <= maxj) {
            int midj = (minj + maxj) >>> 1;
            int midPos = segments[midj];

            if (midPos < blockY) {
                minj = midj + 1;
            } else if (midPos > blockY) {
                maxj = midj - 1;
            } else {
                minj = midj + 1;
                break;
            }
        }

        // minj-1 is the containing segment, or -1 if we're off the bottom
        int j = minj - 1;
        if (j < 0) {
            setOpacityWithSegmentsBelowBottom(xzIndex, blockY, isOpaque);
        } else if (blockY > this.ymax.get(xzIndex)) {
            setOpacityWithSegmentsAboveTop(xzIndex, blockY, isOpaque);
        } else {
            // j is the containing segment, blockY may be at the start
            setOpacityWithSegmentsFor(xzIndex, blockY, j, isOpaque);
        }
    }

    private void setOpacityWithSegmentsBelowBottom(int xzIndex, int blockY, boolean isOpaque) {
        // will the opacity even change?
        if (!isOpaque) {
            return;
        }

        boolean extendsBottomSegmentByOne = blockY == this.ymin[xzIndex] - 1;
        if (extendsBottomSegmentByOne) {
            /*
             ---
             [X]
             ---
             [X]
             --- <-- the current bottom segment starts here
             [#] <-- inserting here
             --- <-- new bottom segment start
             [ ]
              ^ going up
             */
            moveSegmentStartDownAndUpdateMinY(xzIndex, 0);
        } else {
            /*
             ---
             [X]
             ---
             [X]
             --- <-- the current bottom segment starts here, now segment 2
             [ ]
             --- <-- new segment 1, height=blockY + 1, isOpaque=0
             [#] <-- inserting here
             --- <-- new segment 0, height=blockY, isOpaque=1
             [ ]
              ^ going up
             */
            int segment0 = blockY;
            int segment1 = blockY + 1;
            insertSegmentsBelow(xzIndex, 0, segment0, segment1);
            this.ymin[xzIndex] = blockY;
        }
    }

    private void setOpacityWithSegmentsAboveTop(int xzIndex, int blockY, boolean isOpaque) {
        // will the opacity even change?
        if (!isOpaque) {
            return;
        }

        int[] segments = this.segments[xzIndex];
        int lastIndex = getLastSegmentIndex(segments);

        boolean extendsTopSegmentByOne = blockY == this.ymax.get(xzIndex) + 1;
        if (extendsTopSegmentByOne) {
            /*
             [ ]
             --- <-- new ymax
             [#] <-- inserting here
             --- <-- current top segment ends here, limited by ymax
             [X]
             ---
             [X]
              ^ going up
             */
            this.ymax.set(xzIndex, blockY);
        } else {
            /*
             [ ]
             --- <-- limited by newMaxY
             [#] <-- inserting here
             --- <-- new segment [previousLastSegment+2], height=blockY, isOpaque=1
             [ ] <-- possibly many blocks here
             ---
             [ ] <-- block at prevMaxY+1
             --- <-- previously limited by ymax, add segment=[previousLastSegment+1], height=prevMaxY+1, isOpaque=0
             [X] <-- block at prevMaxY
             ---
             [X]
              ^ going up
             */
            int segmentPrevLastPlus1 = this.ymax.get(xzIndex) + 1;
            int segmentPrevLastPlus2 = blockY;
            //insert below the segment above the last segment, so above the last segment
            insertSegmentsBelow(xzIndex, lastIndex + 1, segmentPrevLastPlus1, segmentPrevLastPlus2);
            this.ymax.set(xzIndex, blockY);
        }
    }

    private void setOpacityWithSegmentsFor(int xzIndex, int blockY, int segmentIndexWithBlockY, boolean isOpaque) {
        int[] segments = this.segments[xzIndex];
        int isOpaqueInt = isOpaque ? 1 : 0;

        int segmentWithBlockY = segments[segmentIndexWithBlockY];

        //does it even change anything?
        if (getOpacity(segmentIndexWithBlockY) == isOpaqueInt) {
            return;
        }

        int segmentBottom = segmentWithBlockY;
        int segmentTop = getSegmentTopBlockY(xzIndex, segmentIndexWithBlockY);

        if (segmentTop == segmentBottom) {
            assert segmentBottom == blockY;
            negateOneBlockSegment(xzIndex, segmentIndexWithBlockY);
            return;
        }
        /*
         3 possible cases:
          * change at the top of segment
          * change at the bottom of segment
          * change in the middle of segment
        */
        int lastSegment = getLastSegmentIndex(segments);
        if (blockY == segmentTop) {
            //if it's the top of the top segment - just change ymax
            if (segmentIndexWithBlockY == lastSegment) {
                this.ymax.decrement(xzIndex);
                return;
            }
            /*
             [-]
             ---
             [#] <-- changing this from [X] to [-]
             ---
             [X] <-- segmentWithBlockY
              ^ going up
             */
            moveSegmentStartDownAndUpdateMinY(xzIndex, segmentIndexWithBlockY + 1);
            return;
        }
        if (blockY == segmentBottom) {
            moveSegmentStartUpAndUpdateMinY(xzIndex, segmentIndexWithBlockY);
            return;
        }
        /*
         ---
         [X]
         ---
         [X]
         --- <-- insert this (newSegment2), height=blockY + 1, opacity=!isOpaque
         [#] <-- changing this
         --- <-- insert this (newSegment1), height=blockY, opacity=isOpaque
         [X]
         ---
         [X]
         --- <-- segmentWithBlockY
         [-]
         */
        int newSegment1 = blockY;
        int newSegment2 = blockY + 1;
        insertSegmentsBelow(xzIndex, segmentIndexWithBlockY + 1, newSegment1, newSegment2);
    }

    private void negateOneBlockSegment(int xzIndex, int segmentIndexWithBlockY) {

        int[] segments = this.segments[xzIndex];
        int lastSegmentIndex = getLastSegmentIndex(segments);

        assert lastSegmentIndex >= 2 : "Less than 3 segments in array!";
        if (segmentIndexWithBlockY == lastSegmentIndex) {

            //the top segment must be opaque, so we set it to transparent
            //and the segment below it is also transparent.
            //set both of them to NONE and decrease maxY
            int segmentBelow = segments[segmentIndexWithBlockY - 1];
            this.ymax.set(xzIndex, segmentBelow - 1);
            if (segmentIndexWithBlockY == 2) {
                //after removing top 2 segments we will be left with 1 segment
                //remove them entirely to guarantee at least 3 segments and use min/maxY
                this.segments[xzIndex] = null;
                return;
            }
            segments[segmentIndexWithBlockY] = NONE_SEGMENT;
            segments[segmentIndexWithBlockY - 1] = NONE_SEGMENT;
            assert parityCheck(xzIndex) : "The number of segments was wrong!";
            return;
        }
        if (segmentIndexWithBlockY == 0) {
            //same logic as for top segment applies
            this.ymin[xzIndex] = segments[2];
            if (lastSegmentIndex == 2) {
                this.segments[xzIndex] = null;
                return;
            }
            removeTwoSegments(xzIndex, 0);
            return;
        }
        /*
         The situation:
         # - opacity to set
         - - opposite opacity

         ---
         [#]
         ---
         [#]
         --- <-- old segment=segmentIndexWithBlockY+1, height=blockY+1
         [-]
         --- <-- old segment=segmentIndexWithBlockY, height=blockY, opacity=(-)
         [#]
         ---
         [#]
          ^ going up

          Since this is not the top/bottom segment - we can remove it.
          And to avoid 2 identical segments in a row - remove the one above too
         */
        removeTwoSegments(xzIndex, segmentIndexWithBlockY);
        //but in case after the removal there are less than 3 segments
        //remove them entirely and rely only on min/maxY
        if (lastSegmentIndex == 2) {
            this.segments[xzIndex] = null;
        }
    }

    private void moveSegmentStartUpAndUpdateMinY(int xzIndex, int segmentIndex) {

        // move the segment
        this.segments[xzIndex][segmentIndex] = this.segments[xzIndex][segmentIndex] + 1;

        // move the bottom if needed
        if (segmentIndex == 0) {
            this.ymin[xzIndex]++;
        }
    }

    private void moveSegmentStartDownAndUpdateMinY(int xzIndex, int segmentIndex) {

        // move the segment
        this.segments[xzIndex][segmentIndex] = this.segments[xzIndex][segmentIndex] - 1;

        // move the bottom if needed
        if (segmentIndex == 0) {
            this.ymin[xzIndex]--;
        }
    }

    private void removeTwoSegments(int xzIndex, int firstSegmentToRemove) {

        int[] segments = this.segments[xzIndex];
        int jmax = getLastSegmentIndex(segments);

        // remove the segment
        System.arraycopy(segments, firstSegmentToRemove + 2, segments, firstSegmentToRemove, jmax - 1 - firstSegmentToRemove);
        segments[jmax] = NONE_SEGMENT;
        segments[jmax - 1] = NONE_SEGMENT;
        assert parityCheck(xzIndex) : "The number of segments was wrong!";

        if (segments[0] == NONE_SEGMENT) {
            this.segments[xzIndex] = null;
        }
    }

    //is theIndex = lastSegmentIndex+1, it will be inserted after last segment
    private void insertSegmentsBelow(int xzIndex, int theIndex, int... newSegments) {
        int lastIndex = getLastSegmentIndex(this.segments[xzIndex]);
        int expandSize = newSegments.length;
        //will it fit in current array?
        if (this.segments[xzIndex].length >= lastIndex + expandSize) {
            //shift all segments up
            System.arraycopy(this.segments[xzIndex], theIndex, this.segments[xzIndex], theIndex + expandSize, lastIndex + 1 - theIndex);
            System.arraycopy(newSegments, 0, this.segments[xzIndex], theIndex, expandSize);
            assert parityCheck(xzIndex) : "The number of segments was wrong!";
        } else {
            //need to expand the array
            int[] newSegmentArr = new int[(lastIndex + 1) + expandSize];
            int newArrIndex = 0;
            int oldArrIndex = 0;
            //copy all index up to before theIndex
            for (int i = 0; i < theIndex; i++) {
                newSegmentArr[newArrIndex] = this.segments[xzIndex][oldArrIndex];
                newArrIndex++;
                oldArrIndex++;
            }
            //copy new elements
            for (int i = 0; i < expandSize; i++) {
                newSegmentArr[newArrIndex] = newSegments[i];
                newArrIndex++;
            }
            //copy everything else
            while (newArrIndex < newSegmentArr.length) {
                newSegmentArr[newArrIndex] = this.segments[xzIndex][oldArrIndex];
                newArrIndex++;
                oldArrIndex++;
            }
            this.segments[xzIndex] = newSegmentArr;
            assert parityCheck(xzIndex) : "The number of segments was wrong!";
        }
    }

    private int getSegmentTopBlockY(int xzIndex, int segmentIndex) {
        int[] segments = this.segments[xzIndex];
        //if it's the last segment in the array, or the one above is NoneSegment
        if (segments.length - 1 == segmentIndex || segments[segmentIndex + 1] == NONE_SEGMENT) {
            return this.ymax.get(xzIndex);
        }
        return segments[segmentIndex + 1] - 1;
    }

    private static int getIndex(int localX, int localZ) {
        return (localZ << 4) | localX;
    }

    // Serialization / NBT ---------------------------------------------------------------------------------------------

    public byte[] getData() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);
            writeData(out);
            out.close();
            return buf.toByteArray();
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    public byte[] getDataForClient() {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buf);

            for (int i = 0; i < Cube.SIZE * Cube.SIZE; i++) {
                out.writeInt(ymax.get(i));
            }

            out.close();
            return buf.toByteArray();
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void readData(byte[] data) {
        try {
            ByteArrayInputStream buf = new ByteArrayInputStream(data);
            DataInputStream in = new DataInputStream(buf);
            readData(in);
            in.close();
        } catch (IOException ex) {
            throw new Error(ex);
        }
    }

    private void readData(DataInputStream in) throws IOException {
        for (int i = 0; i < this.segments.length; i++) {
            this.ymin[i] = in.readInt();
            this.ymax.set(i, in.readInt());
            int[] segments = new int[in.readUnsignedShort()];
            if (segments.length == 0) {
                continue;
            }
            for (int j = 0; j < segments.length; j++) {
                segments[j] = in.readInt();
            }
            this.segments[i] = segments;
            assert parityCheck(i) : "The number of segments was wrong!";
        }
    }

    private void writeData(DataOutputStream out) throws IOException {
        for (int i = 0; i < this.segments.length; i++) {
            out.writeInt(this.ymin[i]);
            out.writeInt(this.ymax.get(i));
            int[] segments = this.segments[i];
            if (segments == null || segments.length == 0) {
                out.writeShort(0);
            } else {
                int lastSegmentIndex = getLastSegmentIndex(segments);
                out.writeShort(lastSegmentIndex + 1);
                for (int j = 0; j <= lastSegmentIndex; j++) {
                    out.writeInt(segments[j]);
                }
            }
        }
    }


    // Debug -----------------------------------------------------------------------------------------------------------

    public String dump(int localX, int localZ) {
        int i = getIndex(localX, localZ);

        StringBuilder buf = new StringBuilder();
        buf.append("range=[");
        buf.append(this.ymin[i]);
        buf.append(",");
        buf.append(this.ymax.get(i));
        buf.append("], segments(p,o)=");

        if (this.segments[i] != null) {
            for (int pos : this.segments[i]) {
                int opacity = getOpacity(i);
                buf.append("(");
                buf.append(pos);
                buf.append(",");
                buf.append(opacity);
                buf.append(")");
            }
        }
        return buf.toString();
    }

}
