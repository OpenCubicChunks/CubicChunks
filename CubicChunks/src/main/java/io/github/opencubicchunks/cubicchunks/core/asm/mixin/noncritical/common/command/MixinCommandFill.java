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
package io.github.opencubicchunks.cubicchunks.core.asm.mixin.noncritical.common.command;

import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import io.github.opencubicchunks.cubicchunks.core.world.cube.Cube;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandFill;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(CommandFill.class)
public class MixinCommandFill {

    @Nullable private WeakReference<ICubicWorld> commandWorld;

    //get command sender, can't fail (inject at HEAD
    @Inject(method = "execute", at = @At(value = "HEAD"), require = 1)
    private void getWorldFromExecute(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo cbi) {
        commandWorld = new WeakReference<>((ICubicWorld) sender.getEntityWorld());
    }

    @ModifyConstant(
            method = "execute",
            constant = {
                    @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO, ordinal = 0),
                    @Constant(intValue = 256, ordinal = 0)
            },
            slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=commands.fill.tooManyBlocks")), require = 2)
    private int execute_getMinHeight(int original) {
        if (commandWorld == null) {
            return original;
        }
        ICubicWorld world = commandWorld.get();
        if (world == null) {
            return original;
        }
        return original == 0 ? world.getMinHeight() : world.getMaxHeight();
    }

    // fix that strange vanilla logic for isBlockLoaded
    private Integer minY, maxY;

    @Inject(method = "execute",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/command/ICommandSender;getEntityWorld()Lnet/minecraft/world/World;"),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onGetEntityWorld(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo c,
            BlockPos blockpos, BlockPos blockpos1, Block block, IBlockState iblockstate, BlockPos minPos, BlockPos maxPos, int i) {
        minY = minPos.getY();
        maxY = maxPos.getY();
    }

    @Redirect(method = "execute",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isBlockLoaded(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean isBlockLoadedCheckForHeightRangeRedirect(World world, BlockPos pos) {
        if (!((ICubicWorld) world).isCubicWorld()) {
            return world.isBlockLoaded(pos);
        }
        if (minY == null) {
            assert maxY == null;
            //if the above injection somehow fails, fall back to something reasonable
            return ((ICubicWorld) world).isBlockColumnLoaded(pos);
        }
        for (int blockY = minY; blockY <= maxY; blockY += Cube.SIZE) {
            if (!world.isBlockLoaded(new BlockPos(pos.getX(), blockY, pos.getZ()))) {
                return false;
            }
        }
        return true;
    }
}
