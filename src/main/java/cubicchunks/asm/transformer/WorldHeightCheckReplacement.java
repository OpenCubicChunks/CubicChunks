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
import org.objectweb.asm.Opcodes;

import static cubicchunks.asm.Mappings.WORLD;
import static cubicchunks.asm.Mappings.WORLD_METHODS;
import static cubicchunks.asm.Mappings.WORLD_METHODS_GET_HEIGHT_DESC;
import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

/**
 * General method transformer used to replace height checks.
 * Replaces all of the following:
 * (iflt/ifge) if(anything < 0) {...} --> with if(anything < minHeight) {...}
 * (iflt/ifge) if(anything >= 0) {...} --> with if(anything >= minHeight) {...}
 * (sipuch) 256 --> maxHeight
 * (sipuch) 255 --> maxHeight - 1
 * (iconst_0) 0 --> minHeight
 *
 * In methods where these occur for other reasons than height checks '
 * other more specific trasformer needs to be used.
 */
public class WorldHeightCheckReplacement extends MethodVisitor {
	private static final String WORLD_HEIGHT_ACCESS = "cubicchunks/asm/WorldMethods";

	private boolean transformedLower, transformedUpper;
	public WorldHeightCheckReplacement(MethodVisitor mv) {
		super(Opcodes.ASM4, mv);
	}

	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if(!transformedLower && (opcode == IFLT || opcode == IFGE)) {
			this.loadMinHeight();
			super.visitJumpInsn(opcode == IFLT ? IF_ICMPLT : IF_ICMPGE, label);
			transformedLower = true;
			return;
		}
		super.visitJumpInsn(opcode, label);
	}

	@Override
	public void visitIntInsn(int opcode, int arg) {
		if(!transformedUpper && opcode == Opcodes.SIPUSH) {
			String getMaxHeightDesc = getMethodDescriptor(INT_TYPE, getObjectType(WORLD));

			super.visitVarInsn(ALOAD, 0);
			super.visitMethodInsn(INVOKESTATIC, WORLD_HEIGHT_ACCESS,  "getMaxHeight", getMaxHeightDesc, false);
			if(arg == 255) {
				//height-1
				//i'm not sure if BIPUSH would work here, so to be safe - use LDC and let JVM do it's optimization magic
				super.visitLdcInsn(-1);
				super.visitInsn(IADD);
			}
			transformedUpper = true;
			return;
		}
		super.visitIntInsn(opcode, arg);
	}

	protected void loadMinHeight() {
		super.visitVarInsn(ALOAD, 0);
		super.visitMethodInsn(INVOKESTATIC, WORLD_METHODS, "getMinHeight", WORLD_METHODS_GET_HEIGHT_DESC, false);
	}
}
