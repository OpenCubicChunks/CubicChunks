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
package cubicchunks.worldgen.gui;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;

public class DummyWorld implements IBlockAccess {

    private static DummyWorld instance;
    private IBlockState blockState;
    private BlockPos pos = BlockPos.ORIGIN;

    public DummyWorld() {
        instance = this;
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        if (!pos.equals(this.pos)) {
            return null;
        }
        if (blockState.getBlock().hasTileEntity(blockState)) {
            return blockState.getBlock().createTileEntity(null, blockState);
        }
        return null;
    }

    @Override
    public int getCombinedLight(BlockPos pos, int lightValue) {
        int i = 15;
        int j = 15;
        if (j < lightValue) {
            j = lightValue;
        }
        return i << 20 | i << 4;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        if (pos.equals(this.pos)) {
            return blockState;
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        return getBlockState(pos).getBlock() == Blocks.AIR;
    }

    @Override
    public Biome getBiome(BlockPos pos) {
        return Biome.getBiome(0);
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.DEFAULT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        return true;
    }

    public static IBlockAccess getInstanceWithBlockState(IBlockState iBlockState) {
        if (instance == null) {
            new DummyWorld();
        }
        instance.blockState = iBlockState;
        instance.pos = BlockPos.ORIGIN;
        return instance;
    }

    public static IBlockAccess getInstanceWithBlockStatePos(IBlockState iBlockState, BlockPos pos) {
        if (instance == null) {
            new DummyWorld();
        }
        instance.blockState = iBlockState;
        instance.pos = pos;
        return instance;
    }
}
