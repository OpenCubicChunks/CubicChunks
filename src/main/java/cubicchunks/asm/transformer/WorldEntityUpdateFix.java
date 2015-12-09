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

import static cubicchunks.asm.Mappings.*;
import static org.objectweb.asm.Opcodes.*;

public class WorldEntityUpdateFix extends AbstractMethodTransformer {
	//current number of usages of ICONST_0
	private int numZeroUse = 0;
	private int checkRadiusLoadInt = -1;

	public WorldEntityUpdateFix(MethodVisitor mv) {
		super(ASM4, mv);
	}

	@Override
	public void visitVarInsn(int opcode, int arg) {
		//the first store after the first ICONST_0 use is going to be write to checkRadius local variable
		if(opcode == ISTORE && numZeroUse == 1 && checkRadiusLoadInt == -1) {
			checkRadiusLoadInt = arg;
		}
		super.visitIntInsn(opcode, arg);
	}

	@Override
	public void visitInsn(int opcode) {
		if(opcode == ICONST_0) {
			numZeroUse++;
			//counting from one, zeroes we want to replace are the second and the third one
			if(numZeroUse == 2 || numZeroUse == 3) {
				assert checkRadiusLoadInt != -1;
				System.out.println(checkRadiusLoadInt);
				Label end = new Label();
				Label vanilla = new Label();
				super.visitVarInsn(ALOAD, 0);
				super.visitMethodInsn(INVOKESTATIC, WORLD_METHODS, "isTallWorld", WORLD_METHODS_IS_TALL_WORLD_DESC, false);
				super.visitJumpInsn(IFEQ, vanilla);
				{
					//do cubic chunks stuff
					//load entity
					super.visitVarInsn(ALOAD, 1);
					//get y pos field
					super.visitFieldInsn(GETFIELD, ENTITY, ENTITY_POS_Y, "D");
					super.visitMethodInsn(INVOKESTATIC, MATH_HELPER, MATH_HELPER_FLOOR_DOUBLE, "(D)I", false);
					super.visitVarInsn(ILOAD, checkRadiusLoadInt);
					super.visitInsn(ISUB);
				}
				super.visitJumpInsn(GOTO, end);
				super.visitLabel(vanilla);
				super.visitInsn(opcode);
				super.visitLabel(end);
				if(numZeroUse == 3 && checkRadiusLoadInt >= 0) {
					this.setSuccessful();
				}
				return;
			}
		}
		super.visitInsn(opcode);
	}
}
