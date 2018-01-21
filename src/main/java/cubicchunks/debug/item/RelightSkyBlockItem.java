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
package cubicchunks.debug.item;

import cubicchunks.debug.ItemRegistered;
import cubicchunks.network.PacketCubes;
import cubicchunks.network.PacketDispatcher;
import cubicchunks.util.CubePos;
import cubicchunks.world.ICubeProvider;
import cubicchunks.world.ICubicWorld;
import cubicchunks.world.ICubicWorldServer;
import cubicchunks.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RelightSkyBlockItem extends ItemRegistered {

    public RelightSkyBlockItem(String name) {
        super(name);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing faceHit, float hitX, float hitY,
            float hitZ) {
        ICubicWorld world = (ICubicWorld) worldIn;
        if (!world.isCubicWorld() || world.isRemote()) {
            return EnumActionResult.PASS;
        }
        ICubicWorldServer sworld = (ICubicWorldServer) worldIn;
        
        //serverside
        BlockPos placePos = pos.offset(faceHit);
        sworld.getFirstLightProcessor().debugMode=true;
        Cube cube = sworld.getCubeFromBlockCoords(placePos);
        sworld.getFirstLightProcessor().diffuseSkylight(cube);
        sworld.getFirstLightProcessor().debugMode=false;
        playerIn.sendMessage(new TextComponentString("Successfully updated lighting at " + placePos));
        List<Cube> cubes = new ArrayList<Cube>();
        cubes.add(cube);
        PacketDispatcher.sendTo(new PacketCubes(cubes), (EntityPlayerMP) playerIn);
        return EnumActionResult.PASS;
    }
}