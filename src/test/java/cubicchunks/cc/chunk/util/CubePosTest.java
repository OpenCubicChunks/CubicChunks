package cubicchunks.cc.chunk.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class CubePosTest {

    @Test
    public void asLong() {
        for(int x = -(1<<20); x < (1<<20)-1; x += (1<<20)/500)
        {
            for(int y = -(1<<21); y < (1<<21)-1; y += (1<<21)/500)
            {
                for(int z = -(1<<20); z < (1<<20)-1; z += (1<<20)/500)
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