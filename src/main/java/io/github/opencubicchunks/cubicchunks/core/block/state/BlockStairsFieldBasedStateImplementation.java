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
package io.github.opencubicchunks.cubicchunks.core.block.state;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStairs.EnumHalf;
import net.minecraft.block.BlockStairs.EnumShape;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer.StateImplementation;
import net.minecraft.entity.Entity;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockStairsFieldBasedStateImplementation extends StateImplementation {

    private final IBlockState[] propertyValueArray;
    private final EnumFacing facing; // 6 values
    private final BlockStairs.EnumHalf half; // 2 values
    private final BlockStairs.EnumShape shape; // 5 values

    public BlockStairsFieldBasedStateImplementation(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn, IBlockState[] propertyValueArrayIn) {
        super(blockIn, propertiesIn);
        facing = (EnumFacing) propertiesIn.get(BlockStairs.FACING);
        half = (EnumHalf) propertiesIn.get(BlockStairs.HALF);
        shape = (EnumShape) propertiesIn.get(BlockStairs.SHAPE);
        propertyValueArray = propertyValueArrayIn;
        propertyValueArray[propertyIndex(facing,half,shape)] = this;
    }
    
    private static int propertyIndex(EnumFacing facingIn, BlockStairs.EnumHalf halfIn, BlockStairs.EnumShape shapeIn){
        return facingIn.ordinal() | halfIn.ordinal() << 3 | shapeIn.ordinal() << 4;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Comparable<T>> T getValue(IProperty<T> property) {
        if (property == BlockStairs.FACING)
            return (T) facing;
        if (property == BlockStairs.HALF)
            return (T) half;
        if (property == BlockStairs.SHAPE)
            return (T) shape;
        throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.getBlock().getBlockState());
    }
    
    @Override
    public <T extends Comparable<T>, V extends T> IBlockState withProperty(IProperty<T> property, V value)
    {
        int index = 0;
        if (property == BlockStairs.FACING)
            index = propertyIndex((EnumFacing) value,half,shape);
        else if (property == BlockStairs.HALF)
            index = propertyIndex(facing,(EnumHalf) value,shape);
        else if (property == BlockStairs.SHAPE)
            index = propertyIndex(facing,half,(EnumShape) value);
        else
            throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.getBlock().getBlockState());
        return propertyValueArray[index];
    }
    
    @SuppressWarnings("deprecation")
    @Override
    public void addCollisionBoxToList(World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes,
            @Nullable Entity entityIn, boolean isActualState) {
        int x1 = pos.getX();
        int y1 = pos.getY();
        int z1 = pos.getZ();
        int x2 = x1 + 1;
        int y2 = y1 + 1;
        int z2 = z1 + 1;
        if (entityBox.minX < x2 && entityBox.maxX > x1 && entityBox.minY < y2 && entityBox.maxY > y1 && entityBox.minZ < z2 && entityBox.maxZ > z1)
            this.getBlock().addCollisionBoxToList(this, worldIn, pos, entityBox, collidingBoxes, entityIn, isActualState);
    }

}
