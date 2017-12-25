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
package optifine;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class OptifineDevFix {
    public static void init() {
        try {
            Class<?> reflectorClass = Class.forName("Reflector");
            Class<?> reflectorMethodClass = Class.forName("ReflectorMethod");
            Class<?> reflectorClassClass = Class.forName("ReflectorClass");

            Constructor<?> reflectorMethodConstructor = reflectorMethodClass.getConstructor(reflectorClassClass, String.class, Class[].class);

            Field ForgeBlock_field = reflectorClass.getField("ForgeBlock");
            Field ForgeBlock_getLightValue_field = reflectorClass.getField("ForgeBlock_getLightValue");
            Field ForgeBlock_getLightOpacity_field = reflectorClass.getField("ForgeBlock_getLightOpacity");

            Object ForgeBlock_value = ForgeBlock_field.get(null);

            Object ForgeBlock_getLightValue_newValue = reflectorMethodConstructor.newInstance(ForgeBlock_value, "getLightValue",
                    new Class[]{IBlockState.class, IBlockAccess.class, BlockPos.class});
            Object ForgeBlock_getLightOpacity_newValue = reflectorMethodConstructor.newInstance(ForgeBlock_value, "getLightOpacity",
                    new Class[]{IBlockState.class, IBlockAccess.class, BlockPos.class});

            ForgeBlock_getLightValue_field.set(null, ForgeBlock_getLightValue_newValue);
            ForgeBlock_getLightOpacity_field.set(null, ForgeBlock_getLightOpacity_newValue);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
