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
package cubicchunks.asm;

import com.google.common.collect.Lists;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collection;
import java.util.List;

public class RegionRenderCacheTransformer extends AbstractClassTransformer{

	private static final MethodInfo GET_BLOCK_STATE_RAW = new MethodInfo("func_175631_c", "getBlockStateRaw");

	private static final List<MethodInfo> TO_TRANSFORM = Lists.newArrayList(GET_BLOCK_STATE_RAW);
	@Override
	protected String getTransformedClassName() {
		return "net.minecraft.client.renderer.RegionRenderCache";
	}

	@Override
	protected Collection<MethodInfo> getMethodsToTransform() {
		return TO_TRANSFORM;
	}

	@Override
	protected void transformMethod(MethodInfo methodInfo, MethodNode node) {
		InsnList insns = node.instructions;

		LabelNode startNode = new LabelNode();
		insns.insert(startNode);

		insns.insertBefore(startNode, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.insertBefore(startNode, new VarInsnNode(Opcodes.ALOAD, 1));
		insns.insertBefore(startNode, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/RenderGlobalUtils", "getBlockStateRawFromCache", "(Lnet/minecraft/client/renderer/RegionRenderCache;Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;", false));
		insns.insertBefore(startNode, new InsnNode(Opcodes.DUP));
		insns.insertBefore(startNode, new JumpInsnNode(Opcodes.IFNULL, startNode));
		insns.insertBefore(startNode, new InsnNode(Opcodes.ARETURN));
	}
}
