package cubicchunks.generator.features;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestFeatureGenerator {

	@Test
	public void testGetMinCubeY() {
		final int y1 = 15;
		final int result1 = 0;
		final int y2 = -24;
		final int result2 = -32;
		
		assertEquals(result1, FeatureGenerator.getMinCubeY(y1));
		assertEquals(result2, FeatureGenerator.getMinCubeY(y2));
	}
}
