package io.github.opencubicchunks.cubicchunks.chunk.util;

import java.util.Random;

public class CCMathUtils {

    public static int getRandomPositiveOrNegativeY(Random rand, int bound) {
        if (bound > 0)
            return rand.nextInt(bound);
        else if (bound == 0)
            return bound;
        else
            return -rand.nextInt(Math.abs(bound));
    }
}
