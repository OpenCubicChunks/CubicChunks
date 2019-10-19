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

import io.github.opencubicchunks.cubicchunks.api.world.ICubicWorld;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin(CommandBase.class)
public class MixinCommandBase {

    //I hope there are no threads involved...
    @Nonnull private static WeakReference<ICubicWorld> commandWorld = new WeakReference<>(null);

    //get command sender, can't fail (inject at HEAD)
    @Inject(method = "parseBlockPos", at = @At(value = "HEAD"))
    private static void parseBlockPosPre(ICommandSender sender, String[] args, int startIndex, boolean centerBlock, CallbackInfoReturnable<?> cbi) {
        commandWorld = new WeakReference<>((ICubicWorld) sender.getEntityWorld());
    }

    //modify parseDouble min argument
    @ModifyArg(method = "parseBlockPos",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandBase;parseDouble(DLjava/lang/String;IIZ)D", ordinal = 1),
            index = 2)
    private static int getMinY(int original) {
        ICubicWorld world = commandWorld.get();
        if (world == null) {
            return original;
        }
        return world.getMinHeight();
    }

    //modify parseDouble max argument
    @ModifyArg(method = "parseBlockPos",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/command/CommandBase;parseDouble(DLjava/lang/String;IIZ)D", ordinal = 1),
            index = 3)
    private static int getMaxY(int original) {
        ICubicWorld world = commandWorld.get();
        if (world == null) {
            return original;
        }
        return world.getMaxHeight();
    }
}
