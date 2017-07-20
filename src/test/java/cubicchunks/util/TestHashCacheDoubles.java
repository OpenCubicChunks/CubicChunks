/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015 contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package cubicchunks.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cubicchunks.util.cache.HashCacheDoubles;
import org.junit.Test;

import java.util.Random;
import java.util.function.ToDoubleFunction;

public class TestHashCacheDoubles {

    @Test public void testSingleEntryOneGet() {
        String key = "test";
        ToDoubleFunction<String> source = mock(ToDoubleFunction.class);
        when(source.applyAsDouble(key)).thenReturn(42.0);
        HashCacheDoubles<String> cache = HashCacheDoubles.create(10, source);
        assertEqualsExact(42.0, cache.get("test"));
        assertEqualsExact(42.0, cache.get("test"));
        verify(source, times(1)).applyAsDouble(key);
    }

    @Test public void test() {
        ToDoubleFunction<Integer> source = i -> i * i;
        HashCacheDoubles<Integer> cache = HashCacheDoubles.create(50, source);
        Random rand = new Random();
        for (int i = 0; i < 100000; i++) {
            int randBig = rand.nextInt();
            for (int j = 0; j < 100; j++) {
                int randSmall = rand.nextInt(20);

                int key = randBig + randSmall;
                assertEqualsExact(source.applyAsDouble(key), cache.get(key));
            }
        }
    }

    private void assertEqualsExact(double expected, double value) {
        assertEquals(Double.doubleToLongBits(expected), Double.doubleToLongBits(value));
    }
}
