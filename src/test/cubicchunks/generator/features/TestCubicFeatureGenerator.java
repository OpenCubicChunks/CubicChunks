package cubicchunks.generator.features;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestCubicFeatureGenerator {

	@Test
	public void testGetMinCubeY() {
		final int y1 = 15;
		final int result1 = 0;
		final int y2 = -24;
		final int result2 = -32;
		
		assertEquals(result1, CubicFeatureGenerator.getMinCubeY(y1));
		assertEquals(result2, CubicFeatureGenerator.getMinCubeY(y2));
	}
}
