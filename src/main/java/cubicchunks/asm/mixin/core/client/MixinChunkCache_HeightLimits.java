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
package cubicchunks.asm.mixin.core.client;

import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;

import cubicchunks.asm.MixinUtils;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(ChunkCache.class)
public abstract class MixinChunkCache_HeightLimits {

    @Shadow public World world;

    /**
     * Redirect to modify vanilla height check.
     *
     * @see MixinUtils#getReplacementY(cubicchunks.world.ICubicWorld, BlockPos)
     */
    @Redirect(method = "getLightFor", at = @At(value = "INVOKE", target = BLOCK_POS_GETY), require = 2)
    private int getLightForGetYReplace(BlockPos pos) {
        return MixinUtils.getReplacementY(world, pos);
    }

    /**
     * Redirect to modify vanilla height check.
     *
     * @see MixinUtils#getReplacementY(cubicchunks.world.ICubicWorld, BlockPos)
     */
    @Redirect(method = "getLightForExt", at = @At(value = "INVOKE", target = BLOCK_POS_GETY), require = 2)
    private int getLightForExtGetYReplace(BlockPos pos) {
        return MixinUtils.getReplacementY(world, pos);
    }
}
