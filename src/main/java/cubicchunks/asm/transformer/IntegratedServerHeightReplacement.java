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

import static cubicchunks.asm.Mappings.*;
import static org.objectweb.asm.Opcodes.*;

/**
 * Transforms IntegratedServer constructor.
 */
public class IntegratedServerHeightReplacement extends AbstractMethodTransformer {
	public IntegratedServerHeightReplacement(MethodVisitor mv) {
		super(ASM4, mv);
	}

	@Override
	public void visitEnd() {
		//load this
		super.visitVarInsn(ALOAD, 0);

		//load WorldSettings argument
		super.visitVarInsn(ALOAD, 4);
		//worldSettings.getTerrainType
		super.visitMethodInsn(INVOKEVIRTUAL, WORLD_SETTINGS, WORLD_SETTINGS_GET_TERRAIN_TYPE, WORLD_SETTINGS_GET_TERRAIN_TYPE_DESC, false);
		//WorldMethods.getMaxHeight
		super.visitMethodInsn(INVOKESTATIC, WORLD_METHODS, "getMaxHeight", WORLD_METHODS_GET_MAX_HEIGHT_WORLD_TYPE_DESC, false);

		//stack contains: this, maxHeight
		//this.setBuildLimit();
		super.visitMethodInsn(INVOKEVIRTUAL, MINECRAFT_SERVER, MINECRAFT_SERVER_SET_BUILD_LIMIT, "(I)V", false);
		this.setSuccessful();
	}

}
