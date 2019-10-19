/*
 *  This file is part of Cubic Chunks Mod, licensed under the MIT License (MIT).
 *
 *  Copyright (c) 2015-2019 OpenCubicChunks
 *  Copyright (c) 2015-2019 contributors
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
package io.github.opencubicchunks.cubicchunks.core.util;

import io.github.opencubicchunks.cubicchunks.core.world.BlankEntityContainer;
import io.github.opencubicchunks.cubicchunks.core.CubicChunks;
import io.github.opencubicchunks.cubicchunks.core.world.BlankEntityContainer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.Entity;
import net.minecraft.util.ClassInheritanceMultiMap;

import java.util.ConcurrentModificationException;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ClassInheritanceMultiMapFactory {

    public static final ClassInheritanceMultiMap<Entity>[] EMPTY_ARR = new ClassInheritanceMultiMap[]{new BlankEntityContainer.BlankEntityMap()};

    /**
     * Creates new ClassInheritanceMultiMap without possibility of ConcurrentModificationException
     */
    public static <T> ClassInheritanceMultiMap<T> create(Class<T> c) {
        /*
         * TODO: Check if it's still relevant in future versions
         *
         * This is a hack to workaround a vanilla threading issue.
         * This is bad and should be removed as soon as the issue is fixed
         *
         * The only way to actually fix it is to change vanilla code, which I'm not going to do.
         * Replacing ALL_KNOWN with concurrent hash set won't fix it, synchronization is necessary here.
         * The issue is that one thread may add something to ALL_KNOWN while the constructor in other thread
         * is iterating over the set.
         * It rarely/never happens in vanilla because timings happen to be just right to avoid the issue
         */
        ClassInheritanceMultiMap<T> obj = null;
        do {
            try {
                obj = new ClassInheritanceMultiMap<>(c);
            } catch (ConcurrentModificationException ex) {
                CubicChunks.LOGGER.error("Error creating ClassInheritanceMultiMap, this is threading issue, trying again...", ex);
            }
        } while (obj == null);

        return obj;
    }
}
