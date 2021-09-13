package io.github.opencubicchunks.cubicchunks.levelgen.placement;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PeriodicUserFunctionTest {

    @Test
    public void getValue() {
        PeriodicUserFunction periodicUserFunction = new PeriodicUserFunction.Builder().point(5, 25).point(10, 55).point(15, 105).repeatRange(0, 20).build();

        assertEquals(105, periodicUserFunction.getValue(35), 0.0);
        assertEquals(55, periodicUserFunction.getValue(30), 0.0);
        assertEquals(25, periodicUserFunction.getValue(25), 0.0);

        assertEquals(25, periodicUserFunction.getValue(5), 0.0);
        assertEquals(55, periodicUserFunction.getValue(10), 0.0);
        assertEquals(105, periodicUserFunction.getValue(15), 0.0);

        assertEquals(105, periodicUserFunction.getValue(-5), 0.0);
        assertEquals(55, periodicUserFunction.getValue(-10), 0.0);
        assertEquals(25, periodicUserFunction.getValue(-15), 0.0);

        //Interpolation
        assertEquals(65, periodicUserFunction.getValue(0), 0.0);
        assertEquals(40, periodicUserFunction.getValue(7.5F), 0.0);
        assertEquals(80, periodicUserFunction.getValue(12.5F), 0.0);

        //Repeat test - Positive
        assertEquals(periodicUserFunction.getValue(0), periodicUserFunction.getValue(20), 0.0);
        assertEquals(periodicUserFunction.getValue(7.5F), periodicUserFunction.getValue(27.5F), 0.0);
        assertEquals(periodicUserFunction.getValue(12.5F), periodicUserFunction.getValue(32.5F), 0.0);

        //Repeat Test - Negative
        assertEquals(periodicUserFunction.getValue(0), periodicUserFunction.getValue(-20), 0.0);
        assertEquals(periodicUserFunction.getValue(7.5F), periodicUserFunction.getValue(-20 + 7.5F), 0.0);
        assertEquals(periodicUserFunction.getValue(12.5F), periodicUserFunction.getValue(-20 + 12.5F), 0.0);
    }

    @Test
    public void getValueTest2() {
        PeriodicUserFunction periodicUserFunction = new PeriodicUserFunction.Builder().point(5 - 5, 25).point(10 - 5, 55).point(15 - 5, 105).repeatRange(0 - 5, 20 - 5).build();

        assertEquals(105, periodicUserFunction.getValue(35 - 5), 0.0);
        assertEquals(55, periodicUserFunction.getValue(30 - 5), 0.0);
        assertEquals(25, periodicUserFunction.getValue(25 - 5), 0.0);

        assertEquals(25, periodicUserFunction.getValue(5 - 5), 0.0);
        assertEquals(55, periodicUserFunction.getValue(10 - 5), 0.0);
        assertEquals(105, periodicUserFunction.getValue(15 - 5), 0.0);

        assertEquals(105, periodicUserFunction.getValue(-5 - 5), 0.0);
        assertEquals(55, periodicUserFunction.getValue(-10 - 5), 0.0);
        assertEquals(25, periodicUserFunction.getValue(-15 - 5), 0.0);

        //Interpolation
        assertEquals(65, periodicUserFunction.getValue(0 - 5), 0.0);
        assertEquals(40, periodicUserFunction.getValue(7.5F - 5), 0.0);
        assertEquals(80, periodicUserFunction.getValue(12.5F - 5), 0.0);

        //Repeat test - Positive
        assertEquals(periodicUserFunction.getValue(0 - 5), periodicUserFunction.getValue(20 - 5), 0.0);
        assertEquals(periodicUserFunction.getValue(7.5F - 5), periodicUserFunction.getValue(27.5F - 5), 0.0);
        assertEquals(periodicUserFunction.getValue(12.5F - 5), periodicUserFunction.getValue(32.5F - 5), 0.0);

        //Repeat Test - Negative
        assertEquals(periodicUserFunction.getValue(0 - 5), periodicUserFunction.getValue(-20 - 5), 0.0);
        assertEquals(periodicUserFunction.getValue(7.5F - 5), periodicUserFunction.getValue(-20 + 7.5F - 5), 0.0);
        assertEquals(periodicUserFunction.getValue(12.5F - 5), periodicUserFunction.getValue(-20 + 12.5F - 5), 0.0);
    }


    @Test
    public void trapezoidHeightTest() {

    }
}