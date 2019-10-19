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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.fixes.common;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.api.world.IColumn;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntityEndGateway;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Mixin(TileEntityEndGateway.class)
public class MixinTileEntityEndGateway {

    @Redirect(method = "findExitPortal", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getTopFilledSegment()I"))
    private int getChunkTopFilledSegmentExitFromPortal(Chunk chunk) {
        int top = chunk.getTopFilledSegment();
        if (top < 0) {
            return 0;
        }
        return top;
    }

    @Redirect(method = "findSpawnpointInChunk", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/Chunk;getTopFilledSegment()I"))
    private static int getChunkTopFilledSegmentFindSpawnpoint(Chunk chunk) {
        int top = chunk.getTopFilledSegment();
        if (top < 0) {
            return 0;
        }
        return top;
    }

    /**
     * @author Barteks2x
     * @reason Make it generate cubes with cubic chunks so that it's filled with blocks
     */
    @Overwrite
    private static Chunk getChunk(World world, Vec3d pos) {
        Chunk chunk = world.getChunk(MathHelper.floor(pos.x / Cube.SIZE_D), MathHelper.floor(pos.z / Cube.SIZE_D));
        if (((ICubicWorld) chunk.getWorld()).isCubicWorld()){
            for (int cubeY = 0; cubeY < 16; cubeY++) {
                ((IColumn) chunk).getCube(cubeY);// load the cube
            }
        }
        return chunk;
    }
}
