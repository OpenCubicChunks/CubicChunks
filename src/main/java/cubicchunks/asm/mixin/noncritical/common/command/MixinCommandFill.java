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
import net.minecraft.command.CommandFill;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cubicchunks.asm.JvmNames.BLOCK_POS_GETY;

@Mixin(CommandFill.class)
public class MixinCommandFill {
	private ICommandSender currentSender;

	//get command sender, can't fail (inject at HEAD
	@Inject(method = "execute", at = @At(value = "HEAD"), require = 1)
	private void getWorldFromExecute(MinecraftServer server, ICommandSender sender, String[] args, CallbackInfo cbi) {
		this.currentSender = sender;
	}

	@Redirect(method = "execute", at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal = 6))
	private int getBlockPosYRedirectMin(BlockPos pos) {
		if(currentSender == null) {
			return pos.getY();
		}
		return MixinUtils.getReplacementY(currentSender.getEntityWorld(), pos);
	}

	@Redirect(method = "execute", at = @At(value = "INVOKE", target = BLOCK_POS_GETY, ordinal = 7))
	private int getBlockPosYRedirectMax(BlockPos pos) {
		return MixinUtils.getReplacementY(currentSender.getEntityWorld(), pos);
	}
}
