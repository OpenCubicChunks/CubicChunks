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
package cubicchunks.asm.mixin.noncritical.common.command;

import cubicchunks.asm.MixinUtils;
import net.minecraft.command.CommandCompare;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static cubicchunks.asm.JvmNames.STRUCTURE_BOUNDING_BOX;

@Mixin(CommandCompare.class)
public class MixinCommandCompare {

	@Redirect(method = "execute", at = @At(value = "FIELD", target = STRUCTURE_BOUNDING_BOX + "minY:I", ordinal = 0))
	private int getMinY1(StructureBoundingBox sbb, MinecraftServer server, ICommandSender sender, String[] args) {
		return MixinUtils.getReplacementY(sender.getEntityWorld(), sbb.minY);
	}

	@Redirect(method = "execute", at = @At(value = "FIELD", target = STRUCTURE_BOUNDING_BOX + "minY:I", ordinal = 1))
	private int getMinY2(StructureBoundingBox sbb, MinecraftServer server, ICommandSender sender, String[] args) {
		return MixinUtils.getReplacementY(sender.getEntityWorld(), sbb.minY);
	}

	@Redirect(method = "execute", at = @At(value = "FIELD", target = STRUCTURE_BOUNDING_BOX + "maxY:I", ordinal = 0))
	private int getMaxY1(StructureBoundingBox sbb, MinecraftServer server, ICommandSender sender, String[] args) {
		return MixinUtils.getReplacementY(sender.getEntityWorld(), sbb.maxY);
	}

	@Redirect(method = "execute", at = @At(value = "FIELD", target = STRUCTURE_BOUNDING_BOX + "maxY:I", ordinal = 1))
	private int getMaxY2(StructureBoundingBox sbb, MinecraftServer server, ICommandSender sender, String[] args) {
		return MixinUtils.getReplacementY(sender.getEntityWorld(), sbb.maxY);
	}
}
