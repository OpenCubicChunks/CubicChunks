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

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

/**
 * Replaces hardcoded height checks in World class with getWorldHeight method
 */
public class WorldTransformer implements IClassTransformer{
	private static final String WORLD_CLASS_NAME = "net.minecraft.world.World";

	private static final MethodInfo WORLD_IS_VALID = new MethodInfo("func_175701_a", "isValid");
	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if(!transformedName.equals(WORLD_CLASS_NAME)) {
			return bytes;
		}
		ClassReader cr = new ClassReader(bytes);
		ClassNode cn = new ClassNode(Opcodes.ASM4);
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

		cr.accept(cn, 0);

		transformIsValid(findMethod(cn, WORLD_IS_VALID));

		cn.accept(cw);
		return cw.toByteArray();
	}

	private void transformIsValid(MethodNode cn) {
		InsnList insns = cn.instructions;
		//the y < 0 is the only IFLT Opcode in this method
		AbstractInsnNode ifltInsn = findInsn(insns, JumpInsnNode.class, Opcodes.IFLT, 0);
		//the label we want to jump to is the second label
		LabelNode label = findInsn(insns, LabelNode.class, -1, 1);
		insns.insertBefore(ifltInsn, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.insertBefore(ifltInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/WorldHeightAccess", "getMinHeight", "(Lnet/minecraft/world/World;)I", false));
		insns.set(ifltInsn, new JumpInsnNode(Opcodes.IF_ICMPLT, label));

		AbstractInsnNode sipushInsn = findInsn(insns, IntInsnNode.class, Opcodes.SIPUSH, 0);
		insns.insertBefore(sipushInsn, new VarInsnNode(Opcodes.ALOAD, 0));
		insns.set(sipushInsn, new MethodInsnNode(Opcodes.INVOKESTATIC, "cubicchunks/asm/WorldHeightAccess", "getMaxHeight", "(Lnet/minecraft/world/World;)I", false));
	}

	private <T extends AbstractInsnNode> T findInsn(InsnList insns, Class<T> toFind, int opcode, int occurrence) {
		Iterator<AbstractInsnNode> it = insns.iterator();
		T found = null;
		int count = 0;
		while(it.hasNext()) {
			AbstractInsnNode node = it.next();
			System.out.println(node.getClass() + ", " + node.getOpcode() + ", " + count + ", " + occurrence);
			if(node.getClass() == toFind && node.getOpcode() == opcode) {
				if(count == occurrence) {
					found = (T) node;
					break;
				}
				count++;
			}
		}
		System.out.println(found);
		return found;
	}

	private MethodNode findMethod(ClassNode cn, MethodInfo mi) {
		MethodNode found = null;
		System.out.println("Finding methods");
		for(MethodNode meth : cn.methods) {
			System.out.println("Method: " + meth.name);
			if(WORLD_IS_VALID.sameAs(meth.name)) {
				found = meth;
				break;
			}
		}
		return found;
	}
}
