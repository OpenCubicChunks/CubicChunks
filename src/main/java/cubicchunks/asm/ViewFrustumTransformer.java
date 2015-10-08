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

public class ViewFrustumTransformer extends AbstractClassTransformer {

	private static final MethodInfo GET_RENDER_CHUNK = new MethodInfo("func_178161_a", "getRenderChunk");

	private static final List<MethodInfo> TO_TRANSFORM = Lists.newArrayList(
			GET_RENDER_CHUNK
	);

	@Override
	protected String getTransformedClassName() {
		return "net.minecraft.client.renderer.ViewFrustum";
	}

	@Override
	protected Collection<MethodInfo> getMethodsToTransform() {
		return TO_TRANSFORM;
	}

	@Override
	protected void transformMethod(MethodInfo methodInfo, MethodNode node) {
		transformGetRenderChunk(node);
	}


	private void transformGetRenderChunk(MethodNode node) {
		InsnList insns = node.instructions;

		LabelNode startLabel = new LabelNode();
		insns.insert(startLabel);

		insns.insertBefore(startLabel, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.insertBefore(startLabel, new VarInsnNode(Opcodes.ALOAD, 1));

		insns.insertBefore(startLabel, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/RenderMethods", "getRenderChunk", "(Lnet/minecraft/client/renderer/ViewFrustum;Lnet/minecraft/util/BlockPos;)Lnet/minecraft/client/renderer/chunk/RenderChunk;", false));
		insns.insertBefore(startLabel, new InsnNode(Opcodes.DUP));
		insns.insertBefore(startLabel, new JumpInsnNode(Opcodes.IFNULL, startLabel));
		insns.insertBefore(startLabel, new InsnNode(Opcodes.ARETURN));
	}
}
