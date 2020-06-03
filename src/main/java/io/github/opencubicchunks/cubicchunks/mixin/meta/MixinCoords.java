package io.github.opencubicchunks.cubicchunks.mixin.meta;

import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.utils.MathUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Coords.class)
public class MixinCoords {

    private static final int LOG2_BLOCK_SIZE = MathUtil.log2(ICube.BLOCK_SIZE);

    private static final int posToIndexMask = getPosToIndexMask();
    private static final int indexToPosMask = posToIndexMask >> 4;


    private static int getPosToIndexMask()
    {
        int mask = 0;
        for(int i = ICube.BLOCK_SIZE; i >= 32; i /= 2)
        {
            mask += i;
        }
        return mask;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int blockToLocal(int val) {
        return val & (ICube.BLOCK_SIZE - 1);

        // val & 11111
        //return val & 0x1f;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int blockToCube(int val) {
        return val >> LOG2_BLOCK_SIZE;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int blockCeilToCube(int val) {
        return -((-val) >> LOG2_BLOCK_SIZE);
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int cubeToMinBlock(int val) {
        return val << LOG2_BLOCK_SIZE;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int cubeToMaxBlock(int val) {
        return cubeToMinBlock(val) + (ICube.BLOCK_SIZE-1);
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int blockToIndex32(int x, int y, int z) {
//        Given x pos 33 = 0x21 = 0b0100001
//        1100000
//        0b0100001 & 0b1100000 = 0b0100000
//        0b0100000 >> 4 = 0b10 = 0x2 = 2

//        Given y pos 532 = 0x214 = 0b1000010100
//        0b0001100000
//        0b0001100000
//        0b1000010100 & 0b0001100000 = 0b0
//        0b0 >> 4 = 0b0 = 0x0 = 0

//        Given z pos -921 = -0x399 = 0b1110011001
//        0b0001100000
//        0b0001100000
//        0b1000010100 & 0b0001100000 = 0b0
//        0b0 >> 4 = 0b0 = 0x0 = 0

//        mask needs to be every power of 2 below ICube.BLOCK_SIZE that's > 16

        final int mask = posToIndexMask;
        return (x&mask) >> 4 | (y&mask)>>3 | (z&mask)>>2;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int indexTo32X(int idx) {
        return idx & indexToPosMask;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int indexTo32Y(int idx) {
        return idx >> 1 & indexToPosMask;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int indexTo32Z(int idx) {
        return idx >> 2 & indexToPosMask;
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int sectionToCube(int val) {
        return val >> (MathUtil.log2(ICube.BLOCK_SIZE) - 4);
    }

    /**
     * @author NotStirred
     * @reason Coords overwrite
     */
    @Overwrite
    public static int sectionToCubeViewDistance(int viewDistance) {
        return MathUtil.ceilDiv(viewDistance, ICube.BLOCK_SIZE/16);
    }

}
