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
package cubicchunks.asm.mixin.core.common;

import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;
import static cubicchunks.asm.JvmNames.CHUNK_CACHE_GET_BLOCK_STATE;

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

    @Shadow protected World world;

    /**
     * Redirect to modify vanilla height check.
     *
     * @see MixinUtils#getReplacementY(cubicchunks.world.ICubicWorld, BlockPos)
     */
    @Redirect(method = CHUNK_CACHE_GET_BLOCK_STATE, at = @At(value = "INVOKE", target = BLOCK_POS_GETY), require = 1)
    private int blockPosGetYRedirect(BlockPos pos) {
        return MixinUtils.getReplacementY(world, pos);
    }
}
