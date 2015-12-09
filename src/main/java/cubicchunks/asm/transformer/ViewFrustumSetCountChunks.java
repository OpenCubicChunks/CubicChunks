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

import cubicchunks.util.ReflectionUtil;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static cubicchunks.asm.Mappings.*;
import static org.objectweb.asm.Opcodes.*;

public class ViewFrustumSetCountChunks extends AbstractMethodTransformer {
	public ViewFrustumSetCountChunks(MethodVisitor mv) {
		super(ASM4, mv);
	}

	@Override
	public void visitIntInsn(int opcode, int arg) {
		if(opcode == BIPUSH) {
			Label notCubicChunksLabel = new Label();
			Label afterIfLabel = new Label();
			String worldTypeFieldDesc = ReflectionUtil.getFieldDescriptor(WORLD);
			//get world field
			super.visitVarInsn(ALOAD, 0);
			super.visitFieldInsn(GETFIELD, VIEW_FRUSTUM, VIEW_FRUSTUM_WORLD, worldTypeFieldDesc);
			//is it CubicChunks world?
			super.visitMethodInsn(INVOKESTATIC, WORLD_METHODS, "isTallWorld", WORLD_METHODS_IS_TALL_WORLD_DESC, false);
			//compare to false
			//if not cubic chunks - go to vanilla instructions label
			super.visitJumpInsn(IFEQ, notCubicChunksLabel);
			//cubic chunks
			super.visitVarInsn(ILOAD, 2); //load variable that stores render distance
			super.visitJumpInsn(GOTO, afterIfLabel);

			super.visitLabel(notCubicChunksLabel);
			//vanilla
			super.visitIntInsn(opcode, arg);
			super.visitLabel(afterIfLabel);
			this.setSuccessful();
			return;
		}
		super.visitIntInsn(opcode, arg);
	}
}
