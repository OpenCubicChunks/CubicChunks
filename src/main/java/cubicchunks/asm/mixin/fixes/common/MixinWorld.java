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
package cubicchunks.asm.mixin.fixes.common;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import cubicchunks.util.Coords;
import cubicchunks.world.column.Column;
import cubicchunks.world.cube.Cube;

import static cubicchunks.asm.JvmNames.CHUNK_IS_POPULATED;

/**
 * Currently only fixes markAndNotifyBlock checking if chunk is populated instead of checking cubes.
 */
@Mixin(World.class)
public class MixinWorld {

	// note: markAndNotifyBlock has @Nullable on chunk, this will never be null here,
	// because this isgit lo the chunk on which isPopulated is called
	@Redirect(method = "markAndNotifyBlock", at = @At(value = "INVOKE", target = CHUNK_IS_POPULATED))
	public boolean markNotifyBlock_CubeCheck(Chunk _this,
	                                         BlockPos pos, Chunk chunk, IBlockState oldstate,
	                                         IBlockState newState, int flags) {
		if (!(chunk instanceof Column)) {
			// vanilla compatibility
			return chunk.isPopulated();
		}
		Column column = (Column) chunk;
		Cube cube = column.getCube(Coords.blockToCube(pos.getY()));
		return cube.isFullyPopulated();
	}
}
