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

import mcp.MethodsReturnNonnullByDefault;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReflectionUtil {

    /**
     * Casts any object to inferred type. Useful for reflection.
     *
     * @param in an object
     * @param <T> an inferred type
     * @return the input object, cast to arbitrary type T
     */
    @SuppressWarnings("unchecked") public static <T> T cast(Object in) {
        return (T) in;
    }

    public static <T> Class<? extends T> getClassOrDefault(String name, Class<? extends T> cl) {
        try {
            return cast(Class.forName(name));
        } catch (ClassNotFoundException ex) {
            return cl;
        }
    }

    public static MethodHandle constructHandle(Class<?> owner, Class<?>... args) {
        try {
            Constructor<?> constr = owner.getDeclaredConstructor(args);
            constr.setAccessible(true);
            return MethodHandles.lookup().unreflectConstructor(constr);
        } catch (IllegalAccessException | NoSuchMethodException e) {
            //if it happens - either something has gone horribly wrong or the JVM is blocking access
            throw new Error(e);
        }
    }
}
