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

public class GeneratorStage {
	
	public static GeneratorStage LIGHTING = new GeneratorStage("lighting");
	
	public static GeneratorStage LIVE = new GeneratorStage("live");
	static {
		LIVE.setLastStage();
		LIVE.setOrdinal(Integer.MAX_VALUE);
	}
	
	private final String name;
	
	private boolean isLast;
	
	private int ordinal;
	
	private StageProcessor processor;
	
		
	public GeneratorStage(String name) {
		this.name = name;
		this.isLast = false;
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
	
	
	public void setProcessor(StageProcessor processor) {
		this.processor = processor;
	}

	public StageProcessor getProcessor() {
		return this.processor;
	}
	
	
	public void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}
	
	public int getOrdinal() {
		return this.ordinal;
	}

	public boolean precedes(GeneratorStage other) {
		return this.ordinal < other.ordinal;
	}
}