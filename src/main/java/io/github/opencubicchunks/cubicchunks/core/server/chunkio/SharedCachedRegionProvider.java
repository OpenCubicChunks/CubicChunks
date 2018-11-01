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
package io.github.opencubicchunks.cubicchunks.core.server.chunkio;

import cubicchunks.regionlib.api.region.IRegion;
import cubicchunks.regionlib.api.region.IRegionProvider;
import cubicchunks.regionlib.api.region.key.IKey;
import cubicchunks.regionlib.api.region.key.RegionKey;
import cubicchunks.regionlib.util.CheckedConsumer;
import cubicchunks.regionlib.util.CheckedFunction;
import mcp.MethodsReturnNonnullByDefault;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A region caching provider that uses a shared underlying cache for all instances
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SharedCachedRegionProvider<K extends IKey<K>> implements IRegionProvider<K> {

    private final IRegionProvider<K> sourceProvider;

    private static final Map<SharedCacheKey<?>, IRegion<?>> regionLocationToRegion = new ConcurrentHashMap<>(512);
    private static final int maxCacheSize = 256;

    private boolean closed;

    /**
     * Creates a RegionProvider using the given {@code regionFactory} and {@code maxCacheSize}
     *
     * @param sourceProvider provider used as source of regions
     */
    public SharedCachedRegionProvider(IRegionProvider<K> sourceProvider) {
        this.sourceProvider = sourceProvider;
    }

    @Override
    public <R> Optional<R> fromExistingRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return fromRegion(key, func, false);
    }

    @Override
    public <R> R fromRegion(K key, CheckedFunction<? super IRegion<K>, R, IOException> func) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        return fromRegion(key, func, true).get();
    }

    @Override
    public void forRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        forRegion(key, cons, true);
    }

    @Override
    public void forExistingRegion(K key, CheckedConsumer<? super IRegion<K>, IOException> cons) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        forRegion(key, cons, false);
    }

    @Override public IRegion<K> getRegion(K key) throws IOException {
        SharedCacheKey<?> sharedKey = new SharedCacheKey<>(key.getRegionKey(), sourceProvider);
        IRegion<K> r = (IRegion<K>) regionLocationToRegion.get(sharedKey);
        if (r != null) {
            regionLocationToRegion.remove(sharedKey);
            return r;
        } else {
            return sourceProvider.getRegion(key);
        }
    }

    @Override public Optional<IRegion<K>> getExistingRegion(K key) throws IOException {
        SharedCacheKey<?> sharedKey = new SharedCacheKey<>(key.getRegionKey(), sourceProvider);
        IRegion<K> r = (IRegion<K>) regionLocationToRegion.get(sharedKey);
        if (r != null) {
            regionLocationToRegion.remove(sharedKey);
            return Optional.of(r);
        } else {
            return sourceProvider.getExistingRegion(key);
        }
    }

    @Override public void forAllRegions(CheckedConsumer<? super IRegion<K>, IOException> consumer) throws IOException {
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
        sourceProvider.forAllRegions(consumer);
    }

    @Override public void close() throws IOException {
        synchronized (regionLocationToRegion) {
            if (closed) {
                throw new IllegalStateException("Already closed");
            }
            this.clearRegions();
            this.sourceProvider.close();
            this.closed = true;
        }
    }

    private void forRegion(K location, CheckedConsumer<? super IRegion<K>, IOException> cons, boolean canCreate) throws IOException {
        synchronized (regionLocationToRegion) {
            if (regionLocationToRegion.size() > maxCacheSize) {
                this.clearRegions();
            }

            SharedCacheKey<?> sharedKey = new SharedCacheKey<>(location.getRegionKey(), sourceProvider);
            IRegion<K> region = (IRegion<K>) regionLocationToRegion.get(sharedKey);
            if (region == null) {
                if (canCreate) {
                    region = sourceProvider.getRegion(location);
                } else {
                    region = sourceProvider.getExistingRegion(location).orElse(null);
                }
                if (region != null) {
                    regionLocationToRegion.put(sharedKey, region);
                    cons.accept(region);
                }
            } else {
                cons.accept(region);
            }
        }
    }

    private <R> Optional<R> fromRegion(K location, CheckedFunction<? super IRegion<K>, R, IOException> func, boolean canCreate) throws IOException {
        synchronized (regionLocationToRegion) {
            if (regionLocationToRegion.size() > maxCacheSize) {
                this.clearRegions();
            }

            SharedCacheKey<?> sharedKey = new SharedCacheKey<>(location.getRegionKey(), sourceProvider);
            IRegion<K> region = (IRegion<K>) regionLocationToRegion.get(sharedKey);
            if (region == null) {
                if (canCreate) {
                    region = sourceProvider.getRegion(location);
                } else {
                    region = sourceProvider.getExistingRegion(location).orElse(null);
                }
                if (region != null) {
                    regionLocationToRegion.put(sharedKey, region);
                    return Optional.of(func.apply(region));
                }
            }
            if (region == null) {
                return Optional.empty();
            }
            return Optional.of(func.apply(region));
        }
    }

    public static synchronized void clearRegions() throws IOException {
        Iterator<IRegion<?>> it = regionLocationToRegion.values().iterator();
        while (it.hasNext()) {
            it.next().close();
        }
        regionLocationToRegion.clear();
    }

    private static class SharedCacheKey<K extends IKey<K>> {
        private final RegionKey regionKey;
        private final IRegionProvider<K> regionProvider;

        private SharedCacheKey(RegionKey regionKey, IRegionProvider<K> regionProvider) {
            this.regionKey = regionKey;
            this.regionProvider = regionProvider;
        }

        public RegionKey getRegionKey() {
            return regionKey;
        }

        public IRegionProvider<K> getRegionProvider() {
            return regionProvider;
        }

        @Override public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SharedCacheKey)) {
                return false;
            }

            SharedCacheKey<?> that = (SharedCacheKey<?>) o;

            if (!getRegionKey().equals(that.getRegionKey())) {
                return false;
            }
            if (!getRegionProvider().equals(that.getRegionProvider())) {
                return false;
            }

            return true;
        }

        @Override public int hashCode() {
            int result = getRegionKey().hashCode();
            result = 31 * result + getRegionProvider().hashCode();
            return result;
        }
    }
}
