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

public class RenderGlobalRenderEntities extends AbstractMethodTransformer {

	private int containerLocalRendererInfoVar = -1;

	public RenderGlobalRenderEntities(MethodVisitor mv) {
		super(ASM4, mv);
	}

	@Override
	public void visitVarInsn(int opcode, int var) {
		super.visitVarInsn(opcode, var);
		//the last ALOAD usage before AALOAD that accesses EntityLists accesses containerLocalRenderInformation local variable
		if(opcode == ALOAD)
			this.containerLocalRendererInfoVar = var;
	}


	//I really hope this never breaks...
	@Override
	public void visitInsn(int opcode) {
		if(opcode == AALOAD) {
			if(containerLocalRendererInfoVar < 0) {
				this.setFailed();
				super.visitInsn(opcode);
				return;
			}
			Label endif = new Label();
			Label vanilla = new Label();
			//load world
			super.visitVarInsn(ALOAD, 0);
			super.visitFieldInsn(GETFIELD, RENDER_GLOBAL, RENDER_GLOBAL_THE_WORLD, WORLD_CLIENT_FIELD_DESC);
			super.visitMethodInsn(INVOKESTATIC, WORLD_METHODS, "isTallWorld", WORLD_METHODS_IS_TALL_WORLD_DESC, false);
			//if not cubic chunks (equal to 0) - jump to vanilla
			super.visitJumpInsn(IFEQ, vanilla);
			//if(isCubicChunks
			{
				//at this point we should have an array and int on the stack to that AALOAD would work
				//we don't need them
				super.visitInsn(POP2);
				//empty stack
				super.visitVarInsn(ALOAD, 0);
				super.visitVarInsn(ALOAD, containerLocalRendererInfoVar);
				super.visitFieldInsn(GETFIELD, RG_CONTAINER_LOCAL_RENDER_INFORMATION, RG_CLRI_RENDER_CHUNK, RENDER_CHUNK_FIELD_DESC);
				//stack: [this, renderChunk]
				super.visitMethodInsn(INVOKESTATIC, RENDER_METHODS, "getEntityList", RENDER_METHODS_GET_ENTITY_LIST_DESC, false);
				//go to the end...
				super.visitJumpInsn(GOTO, endif);
			}
			super.visitLabel(vanilla);
			//else
			{
				super.visitInsn(opcode);
			}
			super.visitLabel(endif);
			this.setSuccessful();
			return;
		}
		super.visitInsn(opcode);
	}
}
