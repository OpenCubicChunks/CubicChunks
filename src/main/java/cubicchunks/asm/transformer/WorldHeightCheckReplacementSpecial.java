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

import org.objectweb.asm.MethodVisitor;

import static cubicchunks.asm.Mappings.BLOCK_POS;
import static cubicchunks.asm.Mappings.VEC_3_I_GET_X;
import static org.objectweb.asm.Opcodes.ICONST_0;

/**
 * Special transformer used for methods that use ICONST_0 and it's also used in other placed than height checks.
 * Transformed methods: World.getLightFromNeighborsFor, World.getLightFor
 */
public class WorldHeightCheckReplacementSpecial extends WorldHeightCheckReplacement {
	//transform only after BlockPos.getX() has beeb called
	private boolean getXCalled = false;

	public WorldHeightCheckReplacementSpecial(MethodVisitor mv) {
		super(mv);
	}

	@Override
	public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean iface) {
		if(owner.equals(BLOCK_POS) && name.equals(VEC_3_I_GET_X)) {
			getXCalled = true;
		}
		super.visitMethodInsn(opcode, owner, name, desc, iface);
	}

	@Override
	public void visitInsn(int opcode) {
		if(opcode == ICONST_0) {
			this.loadMinHeight();
			getXCalled = false;
			return;
		}
		super.visitInsn(opcode);
	}
}
