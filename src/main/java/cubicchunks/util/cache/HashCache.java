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
package cubicchunks.util.cache;

import mcp.MethodsReturnNonnullByDefault;

import java.util.function.Function;
import java.util.function.ToIntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class HashCache<K, V> {

    private final V[] cache;
    private final K[] keys;
    private final ToIntFunction<K> hashFunction;
    private final Function<K, V> source;

    @SuppressWarnings("unchecked")
    private HashCache(int size, ToIntFunction<K> hashCode, Function<K, V> source) {
        this.cache = (V[]) new Object[size];
        this.keys = (K[]) new Object[size];
        this.hashFunction = hashCode;
        this.source = source;
    }

    public V get(K key) {
        int index = index(hashFunction.applyAsInt(key));
        if (!key.equals(keys[index])) {
            keys[index] = key;
            cache[index] = source.apply(key);
        }
        return cache[index];
    }

    private int index(int hash) {
        return Math.floorMod(hash, cache.length);
    }

    public static <K, V> HashCache<K, V> create(int size, Function<K, V> source) {
        return create(size, k -> k.hashCode(), source);
    }

    public static <K, V> HashCache<K, V> create(int size, ToIntFunction<K> hashCode, Function<K, V> source) {
        return new HashCache<>(size, hashCode, source);
    }
}
