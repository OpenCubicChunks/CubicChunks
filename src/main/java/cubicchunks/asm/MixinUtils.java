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
package cubicchunks.asm;

import cubicchunks.world.ICubicWorld;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MixinUtils {

    /**
     * This method should be used as a replacement of {@link BlockPos#getY()} when modifying vanilla height check.
     * <p>
     * Most of the time modifying it using {@link org.spongepowered.asm.mixin.injection.ModifyConstant} is not possible
     * because 0 can't be replaced when used in comparison (JVM has separate instruction for it).
     * <p>
     * So instead of trying to fix these height checks, it's eaier to modify value returned by BlockPos#getY() to fit
     * within the vanilla height range when the check should be successful, and be outside of that range otherwise.
     * <p>
     * This hack has some limitations - it can't be used in methods where BlockPos#getY() is used for anything other
     * than a height check since the retuned value would be completely wrong. Fortunately most vanilla methods with
     * hardcoded height checks don't use BlockPos#getY() for anything else.
     *
     * @deprecated Mixin now has expandZeroConditions in ModifyConstant
     */
    @Deprecated
    public static int getReplacementY(ICubicWorld world, BlockPos pos) {
        return getReplacementY(world, pos.getY());
    }

    /**
     * Convenience method with World as argument that calls {@link MixinUtils#getReplacementY(ICubicWorld, BlockPos)}
     *
     * @deprecated Mixin now has expandZeroConditions in ModifyConstant
     */
    @Deprecated
    public static int getReplacementY(World world, BlockPos pos) {
        return getReplacementY((ICubicWorld) world, pos);
    }

    @Deprecated
    public static int getReplacementY(World world, int y) {
        return getReplacementY((ICubicWorld) world, y);
    }

    @Deprecated
    public static int getReplacementY(ICubicWorld world, int y) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return y;
        }
        return 64;
    }

    public static boolean canTickPosition(World world, BlockPos pos) {
        return canTickPosition((ICubicWorld) world, pos);
    }

    public static boolean canTickPosition(World world, BlockPos pos, Predicate<Cube> canTickCube) {
        return canTickPosition((ICubicWorld) world, pos, canTickCube);
    }

    public static boolean canTickPosition(ICubicWorld world, BlockPos pos) {
        return canTickPosition(world, pos, null);
    }

    public static boolean canTickPosition(ICubicWorld world, BlockPos pos, @Nullable Predicate<Cube> canTickCube) {
        if (!world.isValid(pos)) {
            return true; // can tick everything outside of limits
        }
        if (!world.isBlockLoaded(pos)) {
            return false;
        }
        if (canTickCube == null) {
            return true;
        }
        return canTickCube.test(world.getCubeFromBlockCoords(pos));
    }
}
