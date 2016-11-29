package cubicchunks.util;

import org.junit.Test;

import java.util.Random;
import java.util.function.ToDoubleFunction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestHashCacheDoubles {

	@Test public void testSingleEntryOneGet() {
		String key = "test";
		ToDoubleFunction<String> source = mock(ToDoubleFunction.class);
		when(source.applyAsDouble(key)).thenReturn(42.0);
		HashCacheDoubles<String> cache = HashCacheDoubles.create(10, source);
		assertEqualsExact(source.applyAsDouble(key), cache.get("test"));
		assertEqualsExact(source.applyAsDouble(key), cache.get("test"));
		verify(source, times(1));
	}

	@Test public void test() {
		ToDoubleFunction<Integer> source = i -> i*i;
		HashCacheDoubles<Integer> cache = HashCacheDoubles.create(50, source);
		Random rand = new Random();
		for (int i = 0; i < 100000; i++) {
			int randBig = rand.nextInt();
			for (int j = 0; j < 100; j++) {
				int randSmall = rand.nextInt(20);

				int key = randBig+randSmall;
				assertEqualsExact(source.applyAsDouble(key), cache.get(key));
			}
		}
	}

	private void assertEqualsExact(double expected, double value) {
		assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(value));
	}
}
