package io.github.opencubicchunks.cubicchunks.world;

import io.github.opencubicchunks.cubicchunks.chunk.IBigCube;
import net.minecraft.block.BlockState;

import java.util.function.Predicate;

public interface ICubicHeightmap {

    // Vanilla Heightmap -----------------------------------------------------------------------------------------------

    boolean update(int localX, int globalY, int localZ, BlockState blockState);

    int getHeight(int localX, int localZ);

    void setDataArray(long[] data);

    long[] getDataArray();


    // CC Methods ------------------------------------------------------------------------------------------------------

    Predicate<BlockState> getHeightLimitPredicate();

    int getHeightBelow(int localX, int localZ, int globalY);

    int getLowestTopBlockY();

    String dump(int x, int z);


    // This class exists only because I don't want to introduce many off-by-one errors when modifying height tracking code to store
    // height-above-the-top-block instead of height-of-the-top-block (which is done so that the heightmap array can be shared with vanilla)
    final class HeightMap {
        private int[] data;

        public HeightMap() {
            this.data = new int[IBigCube.SECTION_DIAMETER * IBigCube.SECTION_DIAMETER];
        }

        public int get(int index) {
            return data[index] - 1;
        }

        public void set(int index, int value) {
            data[index] = value + 1;
        }

        public void increment(int index) {
            data[index]++;
        }

        public void decrement(int index) {
            data[index]--;
        }
    }
}
