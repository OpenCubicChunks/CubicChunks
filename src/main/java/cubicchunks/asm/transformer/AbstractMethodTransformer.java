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

public abstract class AbstractMethodTransformer extends MethodVisitor {
	//default state is failed
	private TransformationState state = TransformationState.FAILED;

	public AbstractMethodTransformer(int api) {
		super(api);
	}

	public AbstractMethodTransformer(int api, MethodVisitor mv) {
		super(api, mv);
	}

	protected void setFailed() {
		this.state = TransformationState.FAILED;
	}

	protected void setSuccessful() {
		this.state = TransformationState.SUCCESSFUL;
	}

	public boolean isSuccessful() {
		return this.state == TransformationState.SUCCESSFUL;
	}

	public enum TransformationState {
		FAILED,
		SUCCESSFUL
	}
}
