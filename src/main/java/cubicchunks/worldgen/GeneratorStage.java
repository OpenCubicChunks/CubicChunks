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

package cubicchunks.worldgen;

import cubicchunks.util.processor.CubeProcessor;
import cubicchunks.world.dependency.CubeDependencyProvider;
import net.minecraft.world.World;

public abstract class GeneratorStage implements CubeDependencyProvider {

	public static GeneratorStage LIVE = new IndependentGeneratorStage("live");
	static {
		LIVE.setLastStage();
		LIVE.setOrdinal(Integer.MAX_VALUE);
	}
	
	private final String name;
	
	private boolean isLast;
	
	private int ordinal;

	private CubeProcessor processor;


	public GeneratorStage(String name) {
		this.name = name;
		this.isLast = false;
	}
	

	@Override
	public String toString() {
		return this.getName();
	}


	public String getName() {
		return this.name;
	}
	
	
	public void setLastStage() {
		this.isLast = true;
	}

	public boolean isLastStage() {
		return this.isLast;
	}


	public void setProcessor(CubeProcessor processor) {
		this.processor = processor;
	}

	public CubeProcessor getProcessor() {
		return this.processor;
	}


	void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}
	
	int getOrdinal() {
		return this.ordinal;
	}

	public boolean precedes(GeneratorStage other) {
		return this.ordinal < other.ordinal;
	}

	/**
	 * Returns true if cubes that are in this stage (not yet processed with the stage processor)
	 * should be considered as not loaded by {@link World#isAreaLoaded}
	 */
	public boolean isInitialStage() {
		return this.ordinal == 0;
	}
}