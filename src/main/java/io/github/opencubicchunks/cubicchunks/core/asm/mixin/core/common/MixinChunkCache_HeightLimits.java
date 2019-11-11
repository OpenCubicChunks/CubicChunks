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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.core.common;

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Modifies ChunkCache to support extended world height.
 * <p>
 * ChunkCache is used by some AI code and (as subclass of ChunkCache) - block rendering code.
 * getBlockState is used only in AI code.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ChunkCache.class)
public class MixinChunkCache_HeightLimits {

    @Shadow public World world;

    @ModifyConstant(method = "getBlockState",
            constant = @Constant(intValue = 0, expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO, ordinal = 0))
    private int getBlockState_getMinHeight(int orig) {
        return ((ICubicWorld) world).getMinHeight();
    }

    @ModifyConstant(method = "getBlockState", constant = @Constant(intValue = 256))
    private int getBlockState_getMaxHeight(int orig) {
        return ((ICubicWorld) world).getMaxHeight();
    }
    
    public int getMinHeight() {
        return ((ICubicWorld) world).getMinHeight();
    }

    public int getMaxHeight() {
        return ((ICubicWorld) world).getMaxHeight();
    }
}
