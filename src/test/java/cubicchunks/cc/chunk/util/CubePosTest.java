package cubicchunks.cc.chunk.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class CubePosTest {

    @Test
    public void asLong() {
        for(int x = Integer.MIN_VALUE; x < Integer.MAX_VALUE; x += Integer.MAX_VALUE/500)
        {
            for(int y = Integer.MIN_VALUE; y < Integer.MAX_VALUE; y += Integer.MAX_VALUE/500)
            {
                for(int z = Integer.MIN_VALUE; z < Integer.MAX_VALUE; z += Integer.MAX_VALUE/500)
                {
                    Test(x, y, z);
                }
            }
        }
    }

    void Test(int x, int y, int z)
    {
        long pos = CubePos.asLong(x, y, z);
        CubePos cubePos = CubePos.from(pos);
        int x_after = cubePos.getX();
        int y_after = cubePos.getY();
        int z_after = cubePos.getZ();

        assertEquals(x, x_after);
        assertEquals(y, y_after);
        assertEquals(z, z_after);
    }
}