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
package io.github.opencubicchunks.cubicchunks.core.world.cube;

import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.core.lighting.LightingManager;
import io.github.opencubicchunks.cubicchunks.api.util.CubePos;
import io.github.opencubicchunks.cubicchunks.core.util.ticket.TicketList;
import io.github.opencubicchunks.cubicchunks.core.world.BlankEntityContainer;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.chunk.Chunk;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A blank cube, containing no blocks. Any operation on this cube will have no effect. Trying to retrieve blocks will
 * always return {@link Blocks#AIR}
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlankCube extends Cube {

    public BlankCube(Chunk column) {
        super(new TicketList(null), column.getWorld(), column, new CubePos(0, 0, 0),
                Cube.NULL_STORAGE, new BlankEntityContainer(), new HashMap<>(), new ConcurrentLinkedQueue<>(),
                new LightingManager.CubeLightUpdateInfo(null, null) {
                    @Override
                    public void tick() {
                    }
                });
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsBlockPos(BlockPos blockPos) {
        return false;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public IBlockState getBlockState(int blockX, int localOrBlockY, int blockZ) {
        return Blocks.AIR.getDefaultState();
    }

    @Nullable @Override
    public TileEntity getTileEntity(BlockPos pos, Chunk.EnumCreateEntityType creationType) {
        return null;
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onUnload() {
    }

    @Override
    public boolean needsSaving() {
        return false;
    }

    @Override
    public void markSaved() {
    }

    @Override
    public int getLightFor(EnumSkyBlock lightType, BlockPos pos) {
        return lightType.defaultLightValue;
    }

    @Override
    public void setLightFor(EnumSkyBlock lightType, BlockPos pos, int light) {
    }

    @Override
    public void markForRenderUpdate() {
    }
}
