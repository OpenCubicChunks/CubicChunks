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
import net.minecraft.command.CommandClone;
import net.minecraft.command.CommandCompare;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Slice;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Mixin({CommandClone.class, CommandCompare.class})
public class MixinCommandsHeightLimits {

    @Group(name = "command_getMinY", min = 2, max = 2)
    @ModifyConstant(
            method = "execute",
            constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO, ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;minY:I")))
    private int command_getMinY1(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
        return ((ICubicWorld) sender.getEntityWorld()).getMinHeight();
    }

    @Group(name = "command_getMinY")
    @ModifyConstant(
            method = "execute",
            constant = @Constant(expandZeroConditions = Constant.Condition.GREATER_THAN_OR_EQUAL_TO_ZERO, ordinal = 1),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;minY:I")))
    private int command_getMinY2(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
        return ((ICubicWorld) sender.getEntityWorld()).getMinHeight();
    }

    @Group(name = "command_getMaxY", min = 2, max = 2)
    @ModifyConstant(
            method = "execute",
            constant = @Constant(intValue = 256, ordinal = 0),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;maxY:I")))
    private int command_getMaxY1(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
        return ((ICubicWorld) sender.getEntityWorld()).getMaxHeight();
    }

    @Group(name = "command_getMaxY")
    @ModifyConstant(
            method = "execute",
            constant = @Constant(intValue = 256, ordinal = 1),
            slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/gen/structure/StructureBoundingBox;maxY:I")))
    private int command_getMaxY2(int orig, MinecraftServer server, ICommandSender sender, String[] args) {
        return ((ICubicWorld) sender.getEntityWorld()).getMaxHeight();
    }
}
