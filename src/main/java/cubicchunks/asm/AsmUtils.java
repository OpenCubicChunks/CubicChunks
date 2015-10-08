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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Iterator;

class AsmUtils {

	static MethodNode findMethod(ClassNode cn, MethodInfo mi) {
		MethodNode found = null;
		System.out.println("Finding methods");
		for(MethodNode meth : cn.methods) {
			System.out.println("Method: " + meth.name);
			if(mi.sameAs(meth.name)) {
				found = meth;
				break;
			}
		}
		return found;
	}

	static <T extends AbstractInsnNode> T findInsnBefore(InsnList insns, int opcode, AbstractInsnNode instr) {
		int index;

		for(index = insns.indexOf(instr) - 1; insns.get(index).getOpcode() != opcode; --index) {
			//nothing to do here?
		}

		return (T) insns.get(index);

	}

	static MethodInsnNode findMethodCall(InsnList insns, String inCass, MethodInfo method) {
		//find the method call
		MethodInsnNode found = null;
		Iterator<AbstractInsnNode> it = insns.iterator();
		while(it.hasNext()) {
			AbstractInsnNode node = it.next();
			if(node instanceof MethodInsnNode && node.getOpcode() == Opcodes.INVOKEVIRTUAL) {
				MethodInsnNode methodNode = (MethodInsnNode) node;
				if(methodNode.owner.equals(inCass) && method.sameAs(methodNode.name)) {
					found = methodNode;
					break;
				}
			}
		}

		return found;
	}
}
