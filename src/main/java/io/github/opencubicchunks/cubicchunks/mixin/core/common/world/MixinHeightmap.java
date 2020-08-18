package io.github.opencubicchunks.cubicchunks.mixin.core.common.world;

import io.github.opencubicchunks.cubicchunks.CubicChunks;
import io.github.opencubicchunks.cubicchunks.world.ICubicHeightmap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;

import java.io.*;
import java.util.Set;
import java.util.function.Predicate;

import static io.github.opencubicchunks.cubicchunks.chunk.IBigCube.BLOCK_COLUMNS_PER_SECTION;

@Mixin(Heightmap.class)
public class MixinHeightmap implements ICubicHeightmap {

    @Shadow @Final private Predicate<BlockState> heightLimitPredicate;
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
    @Nonnull
    private int[] ymin;

    /**
     * Array containing the y-coordinate of the highest segment in each block column. The value {@link Coords#NO_HEIGHT}
     * is used if a given block column does not contain any segments.
     */
    @Nonnull
    private ICubicHeightmap.HeightMap ymax;

    /**
     * Array containing an array of segments for each x/z position in a column.
     */
    @Nonnull
    private int[][] segments;

    private int heightMapLowest;


    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    public void on$init(IChunk p_i48695_1_, Heightmap.Type p_i48695_2_, CallbackInfo ci) {

        ymin = new int[BLOCK_COLUMNS_PER_SECTION];
        ymax = new ICubicHeightmap.HeightMap();

        segments = new int[BLOCK_COLUMNS_PER_SECTION][];

        // init to empty
        for (int i = 0; i < BLOCK_COLUMNS_PER_SECTION; ++i) {
            ymin[i] = Coords.NO_HEIGHT;
            ymax.set(i, Coords.NO_HEIGHT);
        }
    }


    // Vanilla Heightmap Overrides -------------------------------------------------------------------------------------

    @Override
    public Predicate<BlockState> getHeightLimitPredicate() {
        return heightLimitPredicate;
    }

    /**
     * @author Cyclonit
     */
    @Overwrite @Deprecated
    public static void updateChunkHeightmaps(IChunk chunk, Set<Heightmap.Type> type) {
        // NOP
    }


    /**
     * Handles a block update within a block-column.
     *
     * @param localX The block's x coordinate within its chunk.
     * @param globalY The block's global y coordinate.
     * @param localZ The block's z coordinate within its chunk.
     * @param blockState The updated BlockState.
     * @return True if the update caused a change in this heightmap.
     * @author Cyclonit
     */
    @Overwrite
    public boolean update(int localX, int globalY, int localZ, BlockState blockState) {

        if (globalY > CubicChunks.MAX_SUPPORTED_HEIGHT || globalY < CubicChunks.MIN_SUPPORTED_HEIGHT) {
            return false;
        }

        boolean isOpaque = heightLimitPredicate.test(blockState);
        int xzIndex = getColumnIndex(localX, localZ);

        // we are currently in non-segment mode for the given block-column
        if (segments[xzIndex] == null) {
            return setNoSegments(xzIndex, globalY, isOpaque);
        }
        // otherwise, we use segments
        else {
            return setOpacityWithSegments(xzIndex, globalY, isOpaque);
        }
    }

    /**
     * @param localX The block-column's local x coordinate.
     * @param localZ The block-column's local z coordinate.
     * @return The y-coordinate of the highest block in the given block-column.
     * @author Cyclonit
     */
    @Overwrite
    public int getHeight(int localX, int localZ) {
        return ymax.get(getColumnIndex(localX, localZ));
    }

    /**
     * @param data The data to be set.
     * @author Cyclonit
     */
    @Overwrite
    public void setDataArray(long[] data) {
        setData(toByteArray(data));
    }

    /**
     * @return All of the data of this heightmap.
     * @author Cyclonit
     */
    @Overwrite
    public long[] getDataArray() {
        return toLongArray(getData());
    }


    // ICubicHeightMap -------------------------------------------------------------------------------------------------

    public void updateInRange(int minY, int topY) {



    }

    public int getHeightBelow(int localX, int localZ, int globalY) {

        // within the highest segment or there exists no segment for this block column
        int i = getColumnIndex(localX, localZ);
        if (globalY > ymax.get(i)) {
            return getHeight(localX, localZ);
        }

        // below or at the minimum height, thus there are no blocks below
        if (globalY <= ymin[i]) {
            return Coords.NO_HEIGHT;
        }

        // there is only one opaque segment, everything is opaque from ymin to ymax. globalY is between ymin and ymax,
        // thus the next opaque block below globalY is globalY - 1.
        int[] columnSegments = segments[i];
        if (columnSegments == null) {
            return globalY - 1;
        }

        int segmentIndex = getSegmentIndex(columnSegments, globalY);
        if (segmentIndex < 0) {
            return Coords.NO_HEIGHT;
        }

        boolean blockSegmentIsOpaque = isSegmentOpaque(segmentIndex);

        // The lowest segment is always opaque. Thus, if blockY is in the lowest segment, the next opaque block is
        // at blockY - 1.
        if (segmentIndex == 0) {
            assert blockSegmentIsOpaque : "The bottom opacity segment is transparent!";
            return globalY - 1;
        }

        int blockSegment = columnSegments[segmentIndex];

        // Otherwise, there exists a segment underneath the segment of blockY.

        // If the segment of blockY is transparent, the next opaque block is at the top of the segment underneath.
        if (blockSegmentIsOpaque) {
            return blockSegment - 1;
        }

        // The segment of blockY is opaque, thus, if blockY is not at the bottom of its segment, the next opaque block
        // is at blockY - 1.
        if (globalY != blockSegment) {
            return globalY - 1;
        }

        // If blockY is the lowest block in its segment, the next opaque block is the highest block in the next opaque
        // segment.
        int belowYSegment = columnSegments[segmentIndex - 1];
        return belowYSegment - 1;
    }

    public int getLowestTopBlockY() {
        if (heightMapLowest == Coords.NO_HEIGHT) {
            heightMapLowest = Integer.MAX_VALUE;
            for (int i = 0; i < BLOCK_COLUMNS_PER_SECTION; i++) {
                if (ymax.get(i) < heightMapLowest) {
                    heightMapLowest = ymax.get(i);
                }
            }
            if (heightMapLowest == Coords.NO_HEIGHT) {
                --heightMapLowest; // don't recalculate this on every call
            }
        }
        return heightMapLowest;
    }


    // Serialization ---------------------------------------------------------------------------------------------------

    private static long[] toLongArray(byte[] bytes) {

        int longLength = (int) Math.ceil((float) bytes.length / 4) + 1;
        long[] result = new long[longLength];

        result[0] = bytes.length;

        for (int i = 0; i < bytes.length; ++i) {
            int longIdx = i / 8 + 1;
            int offset = (7 - i % 8) * 8;
            result[longIdx] = result[longIdx] | ((0xFF & (long) bytes[i]) << offset);
        }

        return result;
    }

    private static byte[] toByteArray(long[] longs) {

        int bytesLength = (int) longs[0];
        byte[] result = new byte[bytesLength];

        for (int i = 0; i < bytesLength; ++i) {
            int longIdx = i / 8 + 1;
            int offset = (7 - i % 8) * 8;
            result[i] = (byte) (0xFF & (longs[longIdx] >> offset));
        }

        return result;
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

    public void setData(byte[] data) {
        try {
            ByteArrayInputStream buf = new ByteArrayInputStream(data);
            DataInputStream in = new DataInputStream(buf);
            readData(in);
            in.close();
        } catch (IOException ex) {
            throw new Error(ex);
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


    // Helper ----------------------------------------------------------------------------------------------------------

    private boolean setNoSegments(int xzIndex, int globalY, boolean isOpaque) {
        if (isOpaque) {
            return setNoSegmentsOpaque(xzIndex, globalY);
        } else {
            return setNoSegmentsTransparent(xzIndex, globalY);
        }
    }

    private boolean setNoSegmentsOpaque(int xzIndex, int globalY) {

        // something from nothing?
        if (ymin[xzIndex] == Coords.NO_HEIGHT && ymax.get(xzIndex) == Coords.NO_HEIGHT) {
            ymin[xzIndex] = globalY;
            ymax.set(xzIndex, globalY);
            return true;
        }

        // extending the range?
        if (globalY == ymin[xzIndex] - 1) {
            ymin[xzIndex]--;
            return true;
        } else if (globalY == ymax.get(xzIndex) + 1) {
            ymax.increment(xzIndex);
            return true;
        }

        // make a new section
        //more than one block above ymax?
        if (globalY > ymax.get(xzIndex) + 1) {
            /*
             A visualization of what happens:
             X - already opaque, within min-max
             # - newly set opaque
             [ ]
             --- --> no segment here, new_ymax=globalY
             [#] (block at globalY)
             --- --> segment=2, height=globalY, isOpaque=1
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
            segments[xzIndex] = new int[]{
                ymin[xzIndex],
                ymax.get(xzIndex) + 1,
                globalY
            };
            ymax.set(xzIndex, globalY);
            return true;
        }

        //more than one block below ymin?
        else if (globalY < ymin[xzIndex] - 1) {
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
             --- --> segment=1, height=globalY + 1, isOpaque=0
             [#] (block at blockY)
             --- --> segment=0, height=globalY, isOpaque=1
             [ ]
              ^ going up from there
             */
            segments[xzIndex] = new int[]{
                globalY,
                globalY + 1,
                ymin[xzIndex]
            };
            ymin[xzIndex] = globalY;
            return true;
        }

        // must already be in range
        assert (globalY >= ymin[xzIndex] && globalY <= ymax.get(xzIndex));
        return false;
    }

    private boolean setNoSegmentsTransparent(int xzIndex, int globalY) {

        // nothing into nothing?
        if (ymin[xzIndex] == Coords.NO_HEIGHT && ymax.get(xzIndex) == Coords.NO_HEIGHT) {
            return false;
        }

        assert !(ymin[xzIndex] == Coords.NO_HEIGHT || ymax.get(xzIndex) == Coords.NO_HEIGHT) :
            "Only one of ymin and ymax is NONE! This is not possible";

        // only one block left?
        if (ymax.get(xzIndex) == ymin[xzIndex]) {

            // something into nothing?
            if (globalY == ymin[xzIndex]) {
                ymin[xzIndex] = Coords.NO_HEIGHT;
                ymax.set(xzIndex, Coords.NO_HEIGHT);
                return true;
            }

            // if setting to transparent somewhere else - nothing changes
            return false;
        }

        // out of range?
        if (globalY < ymin[xzIndex] || globalY > ymax.get(xzIndex)) {
            return false;
        }

        // shrinking the range?
        if (globalY == ymin[xzIndex]) {
            ymin[xzIndex]++;
            return true;
        } else if (globalY == ymax.get(xzIndex)) {
            ymax.decrement(xzIndex);
            return true;
        }

        // we must be bisecting the range, need to make segments
        assert (globalY > ymin[xzIndex] && globalY < ymax.get(xzIndex)) :
            String.format("blockY outside of ymin/ymax range: %d -> [%d,%d]", globalY, ymin[xzIndex], ymax.get(xzIndex));
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
            ymin[xzIndex],
            globalY,
            globalY + 1
        };

        return true;
    }

    private boolean setOpacityWithSegments(int xzIndex, int globalY, boolean isOpaque) {

        // binary search to find the insertion point
        int[] columnSegments = segments[xzIndex];
        int segmentIndex = getSegmentIndex(columnSegments, globalY);

        if (segmentIndex < 0) {
            return setOpacityWithSegmentsBelowBottom(xzIndex, globalY, isOpaque);
        } else if (globalY > ymax.get(xzIndex)) {
            return setOpacityWithSegmentsAboveTop(xzIndex, globalY, isOpaque);
        }
        // segmentIndex is the containing segment, blockY may be at the start
        else {
            return setOpacityWithSegmentsFor(xzIndex, globalY, segmentIndex, isOpaque);
        }
    }

    private boolean setOpacityWithSegmentsBelowBottom(int xzIndex, int globalY, boolean isOpaque) {
        // will the opacity even change?
        if (!isOpaque) {
            return false;
        }

        boolean extendsBottomSegmentByOne = globalY == this.ymin[xzIndex] - 1;
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
            insertSegmentsBelow(xzIndex, 0, globalY, globalY + 1);
            ymin[xzIndex] = globalY;
        }

        return true;
    }

    private boolean setOpacityWithSegmentsAboveTop(int xzIndex, int globalY, boolean isOpaque) {
        // will the opacity even change?
        if (!isOpaque) {
            return false;
        }

        int[] columnSegments = segments[xzIndex];
        int lastIndex = getLastSegmentIndex(columnSegments);

        boolean extendsTopSegmentByOne = globalY == ymax.get(xzIndex) + 1;
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
            ymax.set(xzIndex, globalY);
        } else {
            /*
             [ ]
             --- <-- limited by newMaxY
             [#] <-- inserting here
             --- <-- new segment [previousLastSegment+2], height=globalY, isOpaque=1
             [ ] <-- possibly many blocks here
             ---
             [ ] <-- block at prevMaxY+1
             --- <-- previously limited by ymax, add segment=[previousLastSegment+1], height=prevMaxY+1, isOpaque=0
             [X] <-- block at prevMaxY
             ---
             [X]
              ^ going up
             */
            //insert below the segment above the last segment, so above the last segment
            insertSegmentsBelow(xzIndex, lastIndex + 1, ymax.get(xzIndex) + 1, globalY);
            ymax.set(xzIndex, globalY);
        }

        return true;
    }

    private boolean setOpacityWithSegmentsFor(int xzIndex, int globalY, int segmentIndexWithBlockY, boolean isOpaque) {
        int[] columnSegments = segments[xzIndex];
        int segmentContainingBlock = columnSegments[segmentIndexWithBlockY];

        //does it even change anything?
        if (isSegmentOpaque(segmentIndexWithBlockY) == isOpaque) {
            return false;
        }

        int segmentBottom = segmentContainingBlock;
        int segmentTop = getSegmentTopBlockY(xzIndex, segmentIndexWithBlockY);

        if (segmentTop == segmentBottom) {
            assert segmentBottom == globalY;
            negateOneBlockSegment(xzIndex, segmentIndexWithBlockY);
            return true;
        }

        /*
         3 possible cases:
          * change at the top of segment
          * change at the bottom of segment
          * change in the middle of segment
        */
        int lastSegment = getLastSegmentIndex(columnSegments);
        if (globalY == segmentTop) {
            //if it's the top of the top segment - just change ymax
            if (segmentIndexWithBlockY == lastSegment) {
                this.ymax.decrement(xzIndex);
                return true;
            }
            /*
             [-]
             ---
             [#] <-- changing this from [X] to [-]
             ---
             [X] <-- segmentContainingBlock
              ^ going up
             */
            moveSegmentStartDownAndUpdateMinY(xzIndex, segmentIndexWithBlockY + 1);
        }
        else if (globalY == segmentBottom) {
            moveSegmentStartUpAndUpdateMinY(xzIndex, segmentIndexWithBlockY);
        }
        else
        {
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
             --- <-- segmentContainingBlock
             [-]
             */
            insertSegmentsBelow(xzIndex, segmentIndexWithBlockY + 1, globalY, globalY + 1);
        }

        return true;
    }

    private boolean negateOneBlockSegment(int xzIndex, int segmentIndexWithBlockY) {

        int[] columnSegments = segments[xzIndex];
        int lastSegmentIndex = getLastSegmentIndex(columnSegments);

        assert lastSegmentIndex >= 2 : "Less than 3 columnSegments in array!";

        if (segmentIndexWithBlockY == lastSegmentIndex)
        {

            //the top segment must be opaque, so we set it to transparent
            //and the segment below it is also transparent.
            //set both of them to NONE and decrease maxY
            int segmentBelow = columnSegments[segmentIndexWithBlockY - 1];
            ymax.set(xzIndex, segmentBelow - 1);

            if (segmentIndexWithBlockY == 2) {
                //after removing top 2 columnSegments we will be left with 1 segment
                //remove them entirely to guarantee at least 3 columnSegments and use min/maxY
                segments[xzIndex] = null;
            }
            else
            {
                columnSegments[segmentIndexWithBlockY] = NONE_SEGMENT;
                columnSegments[segmentIndexWithBlockY - 1] = NONE_SEGMENT;

                assert parityCheck(xzIndex) : "The number of columnSegments was wrong!";
            }
        }
        else if (segmentIndexWithBlockY == 0)
        {
            //same logic as for top segment applies
            ymin[xzIndex] = columnSegments[2];
            if (lastSegmentIndex == 2) {
                segments[xzIndex] = null;
            } else {
                removeTwoSegments(xzIndex, 0);
            }
        }
        else
        {
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
              And to avoid 2 identical columnSegments in a row - remove the one above too
             */
            removeTwoSegments(xzIndex, segmentIndexWithBlockY);
            //but in case after the removal there are less than 3 columnSegments
            //remove them entirely and rely only on min/maxY
            if (lastSegmentIndex == 2) {
                segments[xzIndex] = null;
            }
        }

        return true;
    }


    private void moveSegmentStartUpAndUpdateMinY(int xzIndex, int segmentIndex) {

        // move the segment
        segments[xzIndex][segmentIndex] = segments[xzIndex][segmentIndex] + 1;

        // move the bottom if needed
        if (segmentIndex == 0) {
            ymin[xzIndex]++;
        }
    }

    private void moveSegmentStartDownAndUpdateMinY(int xzIndex, int segmentIndex) {

        // move the segment
        segments[xzIndex][segmentIndex] = segments[xzIndex][segmentIndex] - 1;

        // move the bottom if needed
        if (segmentIndex == 0) {
            ymin[xzIndex]--;
        }
    }

    private void removeTwoSegments(int xzIndex, int firstSegmentToRemove) {

        int[] columnSegments = segments[xzIndex];
        int jmax = getLastSegmentIndex(columnSegments);

        // remove the segment
        System.arraycopy(columnSegments, firstSegmentToRemove + 2, columnSegments, firstSegmentToRemove, jmax - 1 - firstSegmentToRemove);
        columnSegments[jmax] = NONE_SEGMENT;
        columnSegments[jmax - 1] = NONE_SEGMENT;
        assert parityCheck(xzIndex) : "The number of columnSegments was wrong!";

        if (columnSegments[0] == NONE_SEGMENT) {
            segments[xzIndex] = null;
        }
    }

    private void insertSegmentsBelow(int xzIndex, int theIndex, int... newSegments) {
        int lastIndex = getLastSegmentIndex(segments[xzIndex]);
        int expandSize = newSegments.length;
        //will it fit in current array?
        if (segments[xzIndex].length >= lastIndex + expandSize) {
            //shift all segments up
            System.arraycopy(segments[xzIndex], theIndex, segments[xzIndex], theIndex + expandSize, lastIndex + 1 - theIndex);
            System.arraycopy(newSegments, 0, segments[xzIndex], theIndex, expandSize);
            assert parityCheck(xzIndex) : "The number of segments was wrong!";
        } else {
            //need to expand the array
            int[] newSegmentArr = new int[(lastIndex + 1) + expandSize];
            int newArrIndex = 0;
            int oldArrIndex = 0;
            //copy all index up to before theIndex
            for (int i = 0; i < theIndex; i++) {
                newSegmentArr[newArrIndex] = segments[xzIndex][oldArrIndex];
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
                newSegmentArr[newArrIndex] = segments[xzIndex][oldArrIndex];
                newArrIndex++;
                oldArrIndex++;
            }
            segments[xzIndex] = newSegmentArr;
            assert parityCheck(xzIndex) : "The number of segments was wrong!";
        }
    }


    // Helper ----------------------------------------------------------------------------------------------------------

    private static int getLastSegmentIndex(int[] segments) {
        for (int i = segments.length - 1; i >= 0; i--) {
            if (segments[i] != NONE_SEGMENT) {
                return i;
            }
        }
        throw new Error("Invalid segments state");
    }

    private static int getColumnIndex(int localX, int localZ) {
        return (localZ << 4) | localX;
    }

    /**
     * Returns true if the segment with a given index is opaque. Segments
     * with an even index are always opaque, while those with an odd index
     * are not.
     *
     * @param segmentIndex The segment's index.
     * @return True if the segment is opaque.
     */
    private static boolean isSegmentOpaque(int segmentIndex) {
        return segmentIndex % 2 == 0;
    }

    /**
     * Returns the top most block in a given segment.
     *
     * @param xzIndex The block-column's index.
     * @param segmentIndex The segment's index within the block-column.
     * @return The global y-coordinate of the top most block within the segment.
     */
    private int getSegmentTopBlockY(int xzIndex, int segmentIndex) {
        int[] columnSegments = this.segments[xzIndex];

        //if it's the last segment in the array, or the one above is NoneSegment
        if (columnSegments.length - 1 == segmentIndex || columnSegments[segmentIndex + 1] == NONE_SEGMENT) {
            return ymax.get(xzIndex);
        }
        return columnSegments[segmentIndex + 1] - 1;
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

    /**
     * binary search for the segment containing blockY
     */
    private int getSegmentIndex(int[] segments, int globalY) {

        int mini = 0;
        int maxi = getLastSegmentIndex(segments);

        while (mini <= maxi) {
            int midi = (mini + maxi) >>> 1;
            int midPos = segments[midi];

            if (midPos < globalY) {
                mini = midi + 1;
            } else if (midPos > globalY) {
                maxi = midi - 1;
            } else {
                mini = midi + 1;
                break;
            }
        }

// TODO        assert (mini > 0) : String.format("can't find %d in %s", globalY, dump(segments));

        // The binary search ends on answer + 1, so subtract 1. The result is the index of the segment containing blockY.
        return mini - 1;
    }


    // Debug -----------------------------------------------------------------------------------------------------------

    public String dump(int localX, int localZ) {
        int i = getColumnIndex(localX, localZ);

        StringBuilder buf = new StringBuilder();
        buf.append("range=[");
        buf.append(this.ymin[i]);
        buf.append(",");
        buf.append(this.ymax.get(i));
        buf.append("], segments(p,o)=");

        if (this.segments[i] != null) {
            for (int pos : this.segments[i]) {
                boolean opacity = isSegmentOpaque(i);
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
