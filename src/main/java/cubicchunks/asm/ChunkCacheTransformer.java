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

public class ChunkCacheTransformer extends AbstractClassTransformer{
	private static final MethodInfo GET_BLOCK_STATE = new MethodInfo("func_180495_p", "getBlockState");

	private static final List<MethodInfo> TO_TRANSFORM = Lists.newArrayList(GET_BLOCK_STATE);

	@Override
	protected String getTransformedClassName() {
		return "net.minecraft.world.ChunkCache";
	}

	@Override
	protected Collection<MethodInfo> getMethodsToTransform() {
		return TO_TRANSFORM;
	}

	@Override
	protected void transformMethod(MethodInfo methodInfo, MethodNode node) {
		InsnList insns = node.instructions;
		//the y < 0 is the only IFLT Opcode in this method
		AbstractInsnNode ifltInsn = AsmUtils.findInsn(insns, JumpInsnNode.class, Opcodes.IFLT, 0);
		//the label we want to jump to is the second label
		LabelNode label = AsmUtils.findInsn(insns, LabelNode.class, -1, 1);
		insns.insertBefore(ifltInsn, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.insertBefore(ifltInsn, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/world/ChunkCache", getName("field_72815_e", "worldObj"), "Lnet/minecraft/world/World;"));
		insns.insertBefore(ifltInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/WorldHeightAccess", "getMinHeight", "(Lnet/minecraft/world/World;)I", false));
		insns.set(ifltInsn, new JumpInsnNode(Opcodes.IF_ICMPLT, label));

		AbstractInsnNode sipushInsn = AsmUtils.findInsn(insns, IntInsnNode.class, Opcodes.SIPUSH, 0);
		insns.insertBefore(sipushInsn, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.insertBefore(sipushInsn, new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/world/ChunkCache", getName("field_72815_e", "worldObj"), "Lnet/minecraft/world/World;"));
		insns.set(sipushInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/WorldHeightAccess", "getMaxHeight", "(Lnet/minecraft/world/World;)I", false));
	}
}
