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

import com.google.common.base.Throwables;
import mcp.MethodsReturnNonnullByDefault;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.client.gui.component.container.UIContainer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReflectionUtil {

    @SuppressWarnings("unchecked")
    public static <T> Class<? extends T> getClassOrDefault(String name, Class<? extends T> cl) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException ex) {
            return cl;
        }
    }

    public static MethodHandle getFieldGetterHandle(Class<?> owner, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);
        Field field = getFieldFromSrg(owner, name);
        try {
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (IllegalAccessException e) {
            //if it happens - eighter something has gone horribly wrong or the JVM is blocking access
            throw new Error(e);
        }
    }

    public static MethodHandle getFieldSetterHandle(Class<?> owner, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);
        Field field = getFieldFromSrg(owner, name);
        try {
            return MethodHandles.lookup().unreflectSetter(field);
        } catch (IllegalAccessException e) {
            //if it happens - eighter something has gone horribly wrong or the JVM is blocking access
            throw new Error(e);
        }
    }

    public static MethodHandle getMethodHandle(Class<?> theClass, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);
        try {
            Method method = null;
            for (Method meth : theClass.getDeclaredMethods()) {
                if (name.equals(meth.getName())) {
                    if (method != null) {
                        throw new RuntimeException("Duplicate method names: " + name);
                    }
                    method = meth;
                }
            }
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Sets value of given field
     * <p>
     * Warning: Slow.
     */
    public static void setFieldValueSrg(Object inObject, String srgName, Object newValue) {
        Field field = getFieldFromSrg(inObject.getClass(), srgName);
        removeFinalModifier(field);
        try {
            field.set(inObject, newValue);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Returns value of given field.
     * <p>
     * Warning: Slow.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValueSrg(Object from, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);
        Class<?> cl = from.getClass();
        try {
            Field fld = cl.getDeclaredField(name);
            fld.setAccessible(true);
            return (T) fld.get(from);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void forEachField(Class<?> inClass, Consumer<Field> cons) {
        if (inClass.getSuperclass() != null) {
            forEachField(inClass.getSuperclass(), cons);
        }
        for (Field field : inClass.getDeclaredFields()) {
            removeFinalModifier(field);
            cons.accept(field);
        }
    }

    private static Field getFieldFromSrg(Class<?> owner, String srgName) {
        String name = Mappings.getNameFromSrg(srgName);

        Field foundField = findFieldByName(owner, name);
        foundField.setAccessible(true);
        return foundField;
    }

    private static Field findFieldByName(Class<?> owner, String name) {
        try {
            return owner.getDeclaredField(name);
        } catch (NoSuchFieldException e) {
            return findFieldByName(owner.getSuperclass(), name);
        }
    }

    private static void removeFinalModifier(Field f) {
        f.setAccessible(true);
        int mod = f.getModifiers();
        mod = mod & ~Modifier.FINAL;
        Field modifiersField;
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            throw new AssertionError("Field modifiers not found in class Field", e);
        }
        modifiersField.setAccessible(true);
        try {
            modifiersField.setInt(f, mod);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Cannot set field modifiers in class Field", e);
        }
    }

    public static Set<UIComponent<?>> getField(UIContainer<?> cont, Field componentsField) {
        try {
            return (Set<UIComponent<?>>) componentsField.get(cont);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
