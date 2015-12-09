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
package cubicchunks.asm.transformer;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static cubicchunks.asm.Mappings.WORLD;
import static cubicchunks.asm.Mappings.WORLD_METHODS;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class WorldIsAreaLoadedReplace extends AbstractMethodTransformer {
	public WorldIsAreaLoadedReplace(MethodVisitor mv) {
		super(ASM4, mv);
	}

	@Override
	public void visitCode() {
		String desc = getMethodDescriptor(getType(Boolean.class), getObjectType(WORLD), INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, INT_TYPE, BOOLEAN_TYPE);
		Label vanilla = new Label();
		super.visitCode();
		for(int i = 0; i <= 7; i++) {
			super.visitIntInsn(i == 0 ? ALOAD : ILOAD, i);
		}

		super.visitMethodInsn(INVOKESTATIC, WORLD_METHODS, "isAreaLoaded", desc, false);
		super.visitInsn(DUP);
		super.visitJumpInsn(IFNULL, vanilla);
		super.visitMethodInsn(INVOKEVIRTUAL, Boolean.class.getCanonicalName().replace(".", "/"), "booleanValue", "()Z", false);
		super.visitInsn(IRETURN);
		super.visitLabel(vanilla);
		super.visitInsn(POP);
		this.setSuccessful();
	}
}
